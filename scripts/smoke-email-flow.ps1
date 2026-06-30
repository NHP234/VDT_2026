param(
    [string] $InboxBaseUrl = "http://localhost:8080",
    [string] $ChannelBaseUrl = "http://localhost:8081",
    [string] $MailpitBaseUrl = "http://localhost:8025",
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

function Get-MailpitMessages {
    param([string] $Url)

    $response = Invoke-RestMethod -Method Get -Uri "$Url/api/v1/messages" -TimeoutSec 15
    return @($response.messages)
}

Invoke-Health -Url $InboxBaseUrl
Invoke-Health -Url $ChannelBaseUrl

$mailpitStatus = Invoke-WebRequest -Method Get -Uri $MailpitBaseUrl -TimeoutSec 15
if ($mailpitStatus.StatusCode -ne 200) {
    throw "Mailpit UI returned HTTP $($mailpitStatus.StatusCode)"
}

$stamp = Get-Date -Format "yyyyMMddHHmmss"
$fromEmail = "email-smoke-$stamp@example.test"
$messageId = "<email-smoke-$stamp@example.test>"
$subject = "Smoke email subject $stamp"
$inboundContent = "Smoke email inbound $stamp"
$replyContent = "Smoke email reply $stamp"
$correlationId = "email-smoke-$stamp"

$simulatorBody = @{
    providerAccountId = "demo@example.test"
    fromEmail = $fromEmail
    fromDisplayName = "Email Smoke $stamp"
    toEmail = "demo@example.test"
    messageId = $messageId
    subject = $subject
    textContent = $inboundContent
    occurredAt = (Get-Date).ToUniversalTime().ToString("o")
} | ConvertTo-Json

$simulatorResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "$ChannelBaseUrl/simulators/email/events" `
    -ContentType "application/json" `
    -Headers @{ "X-Correlation-Id" = $correlationId } `
    -Body $simulatorBody `
    -TimeoutSec 30

if (-not $simulatorResponse.published) {
    throw "Email simulator did not publish the inbound event."
}

$duplicateResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "$ChannelBaseUrl/simulators/email/events" `
    -ContentType "application/json" `
    -Headers @{ "X-Correlation-Id" = "$correlationId-duplicate" } `
    -Body $simulatorBody `
    -TimeoutSec 30

if ($duplicateResponse.published) {
    throw "Duplicate email simulator event was published."
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
    -FailureMessage "Inbound email conversation for $fromEmail was not visible in Inbox." `
    -Condition {
        $page = Invoke-RestMethod `
            -Method Get `
            -Uri "$InboxBaseUrl/api/v1/conversations?channel=EMAIL&search=$fromEmail&size=5" `
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
    throw "Inbound email message $messageId was not visible in conversation detail."
}

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
    throw "Outbound email reply was not queued."
}

$sentMessage = Wait-Until `
    -TimeoutSeconds $TimeoutSeconds `
    -FailureMessage "Outbound email reply $($outboundMessage.id) was not marked SENT." `
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

$mailpitMessage = Wait-Until `
    -TimeoutSeconds $TimeoutSeconds `
    -FailureMessage "Mailpit did not receive the SMTP reply for $fromEmail." `
    -Condition {
        return Get-MailpitMessages -Url $MailpitBaseUrl |
            Where-Object {
                ($_.Subject -eq "Re: $subject" -or $_.subject -eq "Re: $subject") -and
                (($_.To | ConvertTo-Json -Depth 5) -match [regex]::Escape($fromEmail))
            } |
            Select-Object -First 1
    }

[pscustomobject]@{
    correlationId = $correlationId
    fromEmail = $fromEmail
    conversationId = $conversation.id
    inboundMessageId = $inboundMessage.id
    outboundMessageId = $outboundMessage.id
    outboundStatus = $sentMessage.deliveryStatus
    providerMessageId = $sentMessage.externalMessageId
    duplicatePublished = $duplicateResponse.published
    mailpitMessageId = $mailpitMessage.ID
} | ConvertTo-Json
