param(
    [Parameter(Mandatory)][string]$UnsignedApkZip,
    [Parameter(Mandatory)][ValidatePattern('^\d+\.\d+\.\d+$')][string]$Version,
    [Parameter(Mandatory)][ValidateRange(1, 2147483647)][int]$VersionCode,
    [Parameter(Mandatory)][ValidatePattern('^[0-9a-f]{40}$')][string]$Commit,
    [Parameter(Mandatory)][string]$CiRun
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$signingPath = Join-Path $projectRoot '.signing'
$privateKey = Join-Path $signingPath 'practice.logger.p12'
$passwordFile = Join-Path $signingPath 'password.txt'
if (-not (Test-Path $privateKey) -or -not (Test-Path $passwordFile)) { throw 'Restore the permanent local signing material before publishing. Never generate a replacement key for an existing installation.' }
$java = (Get-ChildItem (Join-Path $projectRoot '.tools\jre17') -Recurse -Filter java.exe | Select-Object -First 1).FullName
$signer = (Get-ChildItem (Join-Path $projectRoot '.tools\build-tools35') -Recurse -Filter apksigner.jar | Select-Object -First 1).FullName
$aapt = (Get-ChildItem (Join-Path $projectRoot '.tools\build-tools35') -Recurse -Filter aapt2.exe | Select-Object -First 1).FullName
if (-not $java -or -not $signer -or -not $aapt) { throw 'Local Java and Android signing tools are missing.' }
$distributionRoot = Join-Path $projectRoot 'dist'
$stem = 'practice.logger-' + $Version + '-b' + $VersionCode + '-' + $Commit.Substring(0, 7)
$finalDirectory = Join-Path $distributionRoot $stem
$zipPath = $finalDirectory + '.zip'
if ((Test-Path $zipPath) -or (Test-Path $finalDirectory)) { throw 'This version is already packaged. Preserve it; use a new versionCode for changed builds.' }
$stage = Join-Path $distributionRoot ('.staging-' + [Guid]::NewGuid().ToString())
$inputDirectory = Join-Path $stage 'input'
$outputDirectory = Join-Path $stage 'signed'
New-Item -ItemType Directory -Force -Path $inputDirectory, $outputDirectory | Out-Null
Expand-Archive -LiteralPath $UnsignedApkZip -DestinationPath $inputDirectory
$inputs = @(Get-ChildItem $inputDirectory -Recurse -Filter '*-unsigned.apk')
if ($inputs.Count -ne 1) { throw 'Expected exactly one unsigned release APK in CI artifact.' }
$apkPath = Join-Path $outputDirectory ($stem + '.apk')
& $java -jar $signer sign --ks $privateKey --ks-key-alias practice.logger --ks-pass ('file:' + $passwordFile) --out $apkPath $inputs[0].FullName
if ($LASTEXITCODE -ne 0) { throw 'APK signing failed' }
$verification = & $java -jar $signer verify --verbose --print-certs $apkPath
if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed' }
$expectedCertificate = (Get-Content (Join-Path $PSScriptRoot 'signing-certificate.sha256') -Raw).Trim().ToLowerInvariant()
$digestLine = ($verification | Select-String 'Signer #1 certificate SHA-256 digest:').Line
$actualCertificate = ($digestLine -split ': ')[-1].Trim().ToLowerInvariant()
if ($actualCertificate -ne $expectedCertificate) { throw 'Signing identity differs from the pinned permanent certificate.' }
$badging = & $aapt dump badging $apkPath
if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect signed APK metadata' }
$packageLine = ($badging | Select-String '^package:').Line
if ($packageLine -notmatch "name='com.mrayover.practicelogger.app'" -or $packageLine -notmatch ("versionCode='" + $VersionCode + "'") -or $packageLine -notmatch ("versionName='" + [Regex]::Escape($Version) + "'")) { throw 'APK package/version does not match the requested distribution.' }
$apkHash = (Get-FileHash -LiteralPath $apkPath -Algorithm SHA256).Hash.ToLowerInvariant()
$manifest = [ordered]@{
    applicationId = 'com.mrayover.practicelogger.app'
    version = $Version
    versionCode = $VersionCode
    sourceCommit = $Commit
    ciRun = $CiRun
    signingCertificateSha256 = $actualCertificate
    apkSha256 = $apkHash
    packagedAtUtc = [DateTime]::UtcNow.ToString('o')
}
$manifest | ConvertTo-Json | Set-Content (Join-Path $outputDirectory 'BUILD_INFO.json') -Encoding utf8
@"
practice.logger $Version (build $VersionCode)

Open the APK on your Android phone to install. This permanent-signing app has ID
com.mrayover.practicelogger.app and initially installs alongside the temporary-key
0.1.0 catalog. The old app and its data stay intact; Blocks do not transfer automatically.
The launcher label is practice.logger; Android app details show this version.

Future locally signed APKs from this permanent key update this installation in place
and retain its data. Install updates over the existing app; do not uninstall it first.
Never use the temporary-key CI debug APK as an update to this installation.

Source: $Commit
Verification: $CiRun
APK SHA-256: $apkHash
"@ | Set-Content (Join-Path $outputDirectory 'INSTALL.txt') -Encoding utf8
Compress-Archive -Path (Join-Path $outputDirectory '*') -DestinationPath $zipPath
Expand-Archive -LiteralPath $zipPath -DestinationPath $finalDirectory
if ((Get-FileHash (Join-Path $finalDirectory ($stem + '.apk')) -Algorithm SHA256).Hash.ToLowerInvariant() -ne $apkHash) { throw 'ZIP round-trip checksum mismatch' }
Write-Output ('ZIP: ' + $zipPath)
Write-Output ('APK: ' + (Join-Path $finalDirectory ($stem + '.apk')))
Write-Output ('Certificate SHA-256: ' + $actualCertificate)
Write-Output ('APK SHA-256: ' + $apkHash)
