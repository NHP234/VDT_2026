param(
    [string] $InboxBaseUrl = "http://localhost:8080",
    [string] $ChannelBaseUrl = "http://localhost:8081",
    [string] $AgentEmail = "agent@example.test",
    [string] $AgentPassword = "change-me-local-only",
    [int] $TimeoutSeconds = 90
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

function Invoke-Health {
    param([string] $Url)

    $response = Invoke-RestMethod -Method Get -Uri "$Url/actuator/health" -TimeoutSec 30
    if ($response.status -ne "UP") {
        throw "Health endpoint for $Url returned $($response.status)"
    }
}

Invoke-Health -Url $InboxBaseUrl
Invoke-Health -Url $ChannelBaseUrl

$stamp = Get-Date -Format "yyyyMMddHHmmss"
$senderId = "fb-smoke-$stamp"
$messageId = "mid.smoke.$stamp"
$correlationId = "smoke-$stamp"
$inboundContent = "Smoke inbound $stamp"

$simulatorBody = @{
    type = "MESSENGER_MESSAGE"
    pageId = "local-page-id"
    senderId = $senderId
    senderDisplayName = "Smoke User"
    messageId = $messageId
    content = $inboundContent
    occurredAt = (Get-Date).ToUniversalTime().ToString("o")
} | ConvertTo-Json

$simulatorResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "$ChannelBaseUrl/simulators/facebook/events" `
    -ContentType "application/json" `
    -Headers @{ "X-Correlation-Id" = $correlationId } `
    -Body $simulatorBody `
    -TimeoutSec 30

if (-not $simulatorResponse.published) {
    throw "Facebook simulator did not publish the inbound event."
}

$loginResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "$InboxBaseUrl/api/v1/auth/login" `
    -ContentType "application/json" `
    -Body (@{ email = $AgentEmail; password = $AgentPassword } | ConvertTo-Json) `
    -TimeoutSec 15

$authHeaders = @{ Authorization = "Bearer $($loginResponse.accessToken)" }

$conversation = Wait-Until `
    -TimeoutSeconds $TimeoutSeconds `
    -FailureMessage "Inbound conversation for $senderId was not visible in Inbox." `
    -Condition {
        $page = Invoke-RestMethod `
            -Method Get `
            -Uri "$InboxBaseUrl/api/v1/conversations?search=$senderId&size=5" `
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

$inboundMessage = @($detail.messages) |
    Where-Object { $_.externalMessageId -eq $messageId } |
    Select-Object -First 1

if (-not $inboundMessage) {
    throw "Inbound message $messageId was not visible in the conversation detail."
}

$replyContent = "Smoke reply $stamp"
$replyDetail = Invoke-RestMethod `
    -Method Post `
    -Uri "$InboxBaseUrl/api/v1/conversations/$($conversation.id)/replies" `
    -ContentType "application/json" `
    -Headers $authHeaders `
    -Body (@{ content = $replyContent } | ConvertTo-Json) `
    -TimeoutSec 15

$outboundMessage = @($replyDetail.messages) |
    Where-Object { $_.direction -eq "OUTBOUND" -and $_.content -eq $replyContent } |
    Select-Object -First 1

if (-not $outboundMessage) {
    throw "Outbound reply was not queued."
}

$sentMessage = Wait-Until `
    -TimeoutSeconds $TimeoutSeconds `
    -FailureMessage "Outbound reply $($outboundMessage.id) was not marked SENT." `
    -Condition {
        $latestDetail = Invoke-RestMethod `
            -Method Get `
            -Uri "$InboxBaseUrl/api/v1/conversations/$($conversation.id)" `
            -Headers $authHeaders `
            -TimeoutSec 15
        return @($latestDetail.messages) |
            Where-Object { $_.id -eq $outboundMessage.id -and $_.deliveryStatus -eq "SENT" } |
            Select-Object -First 1
    }

[pscustomobject]@{
    correlationId = $correlationId
    senderId = $senderId
    conversationId = $conversation.id
    inboundMessageId = $inboundMessage.id
    outboundMessageId = $outboundMessage.id
    outboundStatus = $sentMessage.deliveryStatus
    providerMessageId = $sentMessage.externalMessageId
} | ConvertTo-Json
