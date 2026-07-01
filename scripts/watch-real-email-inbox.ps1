param(
    [Parameter(Mandatory = $true)]
    [string] $Search,
    [string] $InboxBaseUrl = "http://localhost:8080",
    [string] $AgentEmail = "agent@example.test",
    [string] $AgentPassword = "change-me-local-only",
    [int] $TimeoutSeconds = 120
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Wait-Until {
    param(
        [scriptblock] $Condition,
        [string] $FailureMessage,
        [int] $TimeoutSeconds = 60
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $result = & $Condition
        if ($result) {
            return $result
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    throw $FailureMessage
}

$health = Invoke-RestMethod -Method Get -Uri "$InboxBaseUrl/actuator/health" -TimeoutSec 15
if ($health.status -ne "UP") {
    throw "Inbox service is not UP. Current status: $($health.status)"
}

$loginResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "$InboxBaseUrl/api/v1/auth/login" `
    -ContentType "application/json" `
    -Body (@{ email = $AgentEmail; password = $AgentPassword } | ConvertTo-Json) `
    -TimeoutSec 15

$authHeaders = @{ Authorization = "Bearer $($loginResponse.accessToken)" }
$encodedSearch = [System.Uri]::EscapeDataString($Search)

$conversation = Wait-Until `
    -TimeoutSeconds $TimeoutSeconds `
    -FailureMessage "No EMAIL conversation matching '$Search' appeared within $TimeoutSeconds seconds." `
    -Condition {
        $page = Invoke-RestMethod `
            -Method Get `
            -Uri "$InboxBaseUrl/api/v1/conversations?channel=EMAIL&search=$encodedSearch&size=5" `
            -Headers $authHeaders `
            -TimeoutSec 15
        $items = @($page.content)
        if ($items.Count -gt 0) {
            return $items[0]
        }
        return $null
    }

$detail = Invoke-RestMethod `
    -Method Get `
    -Uri "$InboxBaseUrl/api/v1/conversations/$($conversation.id)" `
    -Headers $authHeaders `
    -TimeoutSec 15

[pscustomobject]@{
    conversationId = $conversation.id
    customerName = $conversation.customerDisplayName
    channel = $conversation.channel
    status = $conversation.status
    lastMessagePreview = $conversation.lastMessagePreview
    messageCount = @($detail.messages).Count
} | ConvertTo-Json
