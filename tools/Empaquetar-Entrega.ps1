$ErrorActionPreference = 'Stop'
$raizEntrega = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Push-Location $raizEntrega
try {
    # Solo fuentes conocidas: nunca archivar el directorio completo ni git archive,
    # porque data.xml y algunos archivos del IDE siguen versionados.
    $versionados = @(git ls-files)
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar Git.' }
    $adicionales = @(
        'Backend/README-A6-AUDITORIA.md',
        'README-A6-ENTREGA.md',
        'tools/Empaquetar-Entrega.ps1',
        'tools/a6/ClienteRealA6.java',
        'tools/a6/ValidacionRealA6.java',
        'tools/a6/VerificarJarA6.java',
        'tools/a6/resultado-2026-11-05.txt'
    )
    $archivos = @(@($versionados) + $adicionales | Sort-Object -Unique | Where-Object {
        $_ -match '^(Protocol|Backend|Frontend)/(src/.*\.(java|form|properties\.example)|sql/[^/]+\.sql|pom\.xml|README[^/]*\.md)$' -or
        $_ -match '^src/(main|test)/java/.*\.java$' -or
        $_ -in @('pom.xml', '.gitignore', 'PROJECT_CONTEXT.md') -or $_ -in $adicionales
    })
    foreach ($relativo in $archivos) {
        $absoluto = [IO.Path]::GetFullPath((Join-Path $raizEntrega $relativo))
        if (-not $absoluto.StartsWith($raizEntrega + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
            throw 'Ruta fuera del repositorio.'
        }
        if (-not (Test-Path -LiteralPath $absoluto -PathType Leaf)) { throw "Falta archivo de entrega: $relativo" }
        if ($relativo -match '(^|/)(data\.xml|\.idea|target|\.git|\.env)(/|$)|\.properties$') {
            throw 'Un archivo privado no debe formar parte de la entrega.'
        }
    }
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $directorioSalida = Join-Path $raizEntrega 'target/entrega'
    New-Item -ItemType Directory -Force $directorioSalida | Out-Null
    $zipEntrega = Join-Path $directorioSalida ('ProyectoII-fuentes-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.zip')
    $zip = [IO.Compression.ZipFile]::Open($zipEntrega, [IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($relativo in $archivos) {
            [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, (Join-Path $raizEntrega $relativo), $relativo, [IO.Compression.CompressionLevel]::Optimal) | Out-Null
        }
    } finally { $zip.Dispose() }
    $lectura = [IO.Compression.ZipFile]::OpenRead($zipEntrega)
    try {
        if ($lectura.Entries.Count -ne $archivos.Count) { throw 'Conteo incorrecto del ZIP.' }
        if (@($lectura.Entries | Where-Object { $_.FullName -match '(^|/)(data\.xml|\.idea|target|\.git|\.env)(/|$)|\.properties$' }).Count -ne 0) {
            throw 'El ZIP contiene archivos privados.'
        }
    } finally { $lectura.Dispose() }
    Write-Output "ZIP verificado: $zipEntrega"
    Write-Output "Archivos: $($archivos.Count). No incluye credenciales locales, data.xml, IDE ni binarios."
} finally { Pop-Location }
