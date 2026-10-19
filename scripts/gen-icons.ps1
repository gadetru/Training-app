using namespace System.Drawing
using namespace System.Drawing.Imaging
using namespace System.Drawing.Drawing2D
Add-Type -AssemblyName System.Drawing

<#
.SYNOPSIS
    Genera variantes del logo del launcher + splash a partir de references/plantilla-logo/miLogo.png.
    Requiere: PowerShell 5.1 + System.Drawing (.NET Framework).
    El script SOLO lee miLogo.png (nunca la modifica) y escribe los PNG res/.
#>

$csharp = @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Collections.Generic;
using System.Runtime.InteropServices;

public static class IconGen {
    public static void ClearBgFromEdges(Bitmap bmp, int tol) {
        int w = bmp.Width, h = bmp.Height;
        Rectangle rect = new Rectangle(0, 0, w, h);
        BitmapData bd = bmp.LockBits(rect, ImageLockMode.ReadWrite, bmp.PixelFormat);
        try {
            int stride = bd.Stride;
            int len = stride * h;
            byte[] bytes = new byte[len];
            Marshal.Copy(bd.Scan0, bytes, 0, len);
            byte bgB = bytes[0], bgG = bytes[1], bgR = bytes[2];
            int px = w * h;
            bool[] visited = new bool[px];
            Queue<int> q = new Queue<int>();
            Action<int> seed = p => {
                int idx = p * 4;
                if (!visited[p] && Math.Abs(bytes[idx] - bgB) <= tol && Math.Abs(bytes[idx+1] - bgG) <= tol && Math.Abs(bytes[idx+2] - bgR) <= tol) {
                    visited[p] = true; q.Enqueue(p);
                }
            };
            for (int x = 0; x < w; x++) { seed(x); seed((h - 1) * w + x); }
            for (int y = 0; y < h; y++) { seed(y * w); seed(y * w + w - 1); }
            while (q.Count > 0) {
                int p = q.Dequeue();
                int idx = p * 4;
                bytes[idx + 3] = 0;
                int x = p % w, y = p / w;
                if (x > 0) seed(p - 1);
                if (x < w - 1) seed(p + 1);
                if (y > 0) seed(p - w);
                if (y < h - 1) seed(p + w);
            }
            Marshal.Copy(bytes, 0, bd.Scan0, len);
        } finally { bmp.UnlockBits(bd); }
    }

    public static void ApplyCircularMask(Bitmap bmp) {
        int w = bmp.Width, h = bmp.Height;
        Rectangle rect = new Rectangle(0, 0, w, h);
        BitmapData bd = bmp.LockBits(rect, ImageLockMode.ReadWrite, bmp.PixelFormat);
        try {
            int stride = bd.Stride;
            int len = stride * h;
            byte[] bytes = new byte[len];
            Marshal.Copy(bd.Scan0, bytes, 0, len);
            double cx = w / 2.0, cy = h / 2.0, rx = w / 2.0, ry = h / 2.0;
            for (int y = 0; y < h; y++) {
                int row = y * stride;
                double dy = (y - cy) / ry;
                for (int x = 0; x < w; x++) {
                    int idx = row + x * 4;
                    double dx = (x - cx) / rx;
                    double d2 = dx * dx + dy * dy;
                    if (d2 > 1.0) {
                        bytes[idx + 3] = 0;
                    } else if (d2 > 0.81) {
                        bytes[idx + 3] = (byte)Math.Max(0, Math.Min(255, Math.Round(bytes[idx + 3] * (1.0 - (d2 - 0.81) / 0.19))));
                    }
                }
            }
            Marshal.Copy(bytes, 0, bd.Scan0, len);
        } finally { bmp.UnlockBits(bd); }
    }

    public static Bitmap MakeMonochrome(Bitmap src, Color tone) {
        int w = src.Width, h = src.Height;
        Rectangle rect = new Rectangle(0, 0, w, h);
        BitmapData bd = src.LockBits(rect, ImageLockMode.ReadWrite, src.PixelFormat);
        try {
            int stride = bd.Stride;
            int len = stride * h;
            byte[] bytes = new byte[len];
            Marshal.Copy(bd.Scan0, bytes, 0, len);
            for (int i = 0; i < len; i += 4) {
                if (bytes[i + 3] > 0) {
                    bytes[i]     = tone.B;
                    bytes[i + 1] = tone.G;
                    bytes[i + 2] = tone.R;
                }
            }
            Marshal.Copy(bytes, 0, bd.Scan0, len);
        } finally { src.UnlockBits(bd); }
        return src;
    }
}
'@
Add-Type -TypeDefinition $csharp -ReferencedAssemblies System.Drawing

$repoRoot = Resolve-Path (Split-Path -Parent $PSScriptRoot)
$sourcePath = Join-Path $repoRoot "references\plantilla-logo\miLogo.png"
$resRoot = Join-Path $repoRoot "app\src\main\res"

if (!(Test-Path $sourcePath)) { throw "No se encuentra el origen: $sourcePath" }

function New-HqBitmap {
    param([int]$w, [int]$h)
    New-Object Bitmap($w, $h, [PixelFormat]::Format32bppPArgb)
}

function Scale-Image {
    param([Image]$src, [int]$w, [int]$h)
    $dst = New-HqBitmap $w $h
    $g = [Graphics]::FromImage($dst)
    $g.CompositingMode = [CompositingMode]::SourceCopy
    $g.CompositingQuality = [CompositingQuality]::HighQuality
    $g.InterpolationMode = [InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [SmoothingMode]::AntiAlias
    $g.PixelOffsetMode = [PixelOffsetMode]::HighQuality
    $g.DrawImage($src, 0, 0, $w, $h)
    $g.Dispose()
    $dst
}

function Save-Png {
    param([Image]$img, [string]$path)
    $dir = Split-Path -Parent $path
    if ($dir -and !(Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    $img.Save($path, [ImageFormat]::Png)
}

function Center-In-SafeZone {
    param([Bitmap]$src, [int]$canvasW, [int]$canvasH)
    $dst = New-HqBitmap $canvasW $canvasH
    $safeW = [int]([math]::Round($canvasW * 2 / 3))
    $safeH = [int]([math]::Round($canvasH * 2 / 3))
    $scale = [math]::Min([double]$safeW / $src.Width, [double]$safeH / $src.Height)
    $newW = [int]([math]::Round($src.Width * $scale))
    $newH = [int]([math]::Round($src.Height * $scale))
    $dx = [int]([math]::Round(($canvasW - $newW) / 2))
    $dy = [int]([math]::Round(($canvasH - $newH) / 2))
    $g = [Graphics]::FromImage($dst)
    $g.CompositingMode = [CompositingMode]::SourceOver
    $g.CompositingQuality = [CompositingQuality]::HighQuality
    $g.InterpolationMode = [InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [SmoothingMode]::AntiAlias
    $g.PixelOffsetMode = [PixelOffsetMode]::HighQuality
    $g.DrawImage($src, $dx, $dy, $newW, $newH)
    $g.Dispose()
    $dst
}

# --- Cargar origen y normalizar a 32bpp pArgb ---
$raw = [Bitmap]::FromFile($sourcePath)
$src = New-HqBitmap $raw.Width $raw.Height
$g = [Graphics]::FromImage($src)
$g.CompositingMode = [CompositingMode]::SourceCopy
$g.InterpolationMode = [InterpolationMode]::HighQualityBicubic
$g.DrawImage($raw, 0, 0, $raw.Width, $raw.Height)
$g.Dispose()
$raw.Dispose()
Write-Host ("Origen cargado: {0}x{1}" -f $src.Width, $src.Height)

# Densidades: legacy 48/72/96/144/192 ; foreground adaptive 108/162/216/324/432
$densities = @(
    @{ name = "mipmap-mdpi";    legacy = 48;  fg = 108 },
    @{ name = "mipmap-hdpi";    legacy = 72;  fg = 162 },
    @{ name = "mipmap-xhdpi";   legacy = 96;  fg = 216 },
    @{ name = "mipmap-xxhdpi";  legacy = 144; fg = 324 },
    @{ name = "mipmap-xxxhdpi"; legacy = 192; fg = 432 }
)

# --- Variante 3: transparente (fondo vaciado por inundacion desde bordes) ---
$transparent = New-HqBitmap $src.Width $src.Height
$g = [Graphics]::FromImage($transparent)
$g.CompositingMode = [CompositingMode]::SourceCopy
$g.InterpolationMode = [InterpolationMode]::HighQualityBicubic
$g.DrawImage($src, 0, 0, $src.Width, $src.Height)
$g.Dispose()
[IconGen]::ClearBgFromEdges($transparent, 60)
Write-Host "Transparente listo"

# --- Variante 4: monochrome (una sola tonalidad desde el alfa) ---
$monochromeTone = [Color]::FromArgb(255, 0xFF, 0x5E, 0x00)
$monochrome = [IconGen]::MakeMonochrome($transparent, $monochromeTone)
Write-Host "Monochrome listo"

# --- Generar por densidad ---
foreach ($d in $densities) {
    $folder = Join-Path $resRoot $d.name

    # 1. Completa cuadrada (legacy ic_launcher.png, color completo)
    $legacy = Scale-Image $src $d.legacy $d.legacy
    Save-Png $legacy (Join-Path $folder "ic_launcher.png")
    $legacy.Dispose()

    # 2. Redondeada (legacy ic_launcher_round.png, mascara circular antialias)
    $round = Scale-Image $src $d.legacy $d.legacy
    [IconGen]::ApplyCircularMask($round)
    Save-Png $round (Join-Path $folder "ic_launcher_round.png")
    $round.Dispose()

    # 3. Transparente foreground adaptive (centrado en zona segura)
    $fgScaled = Scale-Image $transparent $d.fg $d.fg
    $fgSafe = Center-In-SafeZone $fgScaled $d.fg $d.fg
    Save-Png $fgSafe (Join-Path $folder "ic_launcher_foreground.png")
    $fgScaled.Dispose()
    $fgSafe.Dispose()
    Write-Host ("Densidad {0} ({1}) generada" -f $d.name, $d.legacy)
}

# Monochrome unico (drawable unico, sin densidad) para icono temático
$monoFolder = Join-Path $resRoot "mipmap-anydpi"
Save-Png $monochrome (Join-Path $monoFolder "ic_launcher_foreground_monochrome.png")
$monochrome.Dispose()

# --- Variante 5: Splash (original 1024x1024 sobre lienzo mayor con margen transparente) ---
$splashCanvas = 1280
$logo = Scale-Image $src 1024 1024
$splash = New-HqBitmap $splashCanvas $splashCanvas
$g = [Graphics]::FromImage($splash)
$g.CompositingMode = [CompositingMode]::SourceCopy
$g.InterpolationMode = [InterpolationMode]::HighQualityBicubic
$g.DrawImage($logo, [int](($splashCanvas - 1024) / 2), [int](($splashCanvas - 1024) / 2), 1024, 1024)
$g.Dispose()
$logo.Dispose()
$splashFolder = Join-Path $resRoot "drawable-nodpi"
Save-Png $splash (Join-Path $splashFolder "splash_logo.png")
$splash.Dispose()

$src.Dispose()
Write-Host "Iconos generados OK en $resRoot"
