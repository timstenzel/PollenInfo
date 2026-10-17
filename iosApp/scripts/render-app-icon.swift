// Renders the iOS app icon (1024 px; default, dark and tinted) from the same geometry as the Android
// adaptive icon, androidApp/src/main/res/drawable/ic_launcher_foreground.xml. Re-run after changing
// that drawable:
//
//   swift iosApp/scripts/render-app-icon.swift iosApp/iosApp/Assets.xcassets/AppIcon.appiconset

import CoreGraphics
import Foundation
import ImageIO
import UniformTypeIdentifiers

let size = 1024
// The adaptive icon's 108-unit canvas, cropped to its central 84 units: the grain (radius 32)
// keeps about the margin a launcher mask leaves around it on Android.
let viewportOrigin = 12.0
let viewportSize = 84.0

let olive = CGColor(srgbRed: 0x5B / 255.0, green: 0x63 / 255.0, blue: 0x1D / 255.0, alpha: 1) // primaryLight
let paleOlive = CGColor(srgbRed: 0xC4 / 255.0, green: 0xCD / 255.0, blue: 0x7B / 255.0, alpha: 1) // primaryDark
let white = CGColor(srgbRed: 1, green: 1, blue: 1, alpha: 1)

/// The grain: six spikes, the body and its three pores, in canvas units (y down, as in the vector).
func grain(in context: CGContext, glyph: CGColor, pores: CGColor?) {
    context.setFillColor(glyph)
    for step in 0..<6 {
        context.saveGState()
        context.translateBy(x: 54, y: 54)
        context.rotate(by: Double(step) * .pi / 3)
        context.translateBy(x: -54, y: -54)
        let spike = CGMutablePath()
        spike.move(to: CGPoint(x: 50.5, y: 35))
        spike.addQuadCurve(to: CGPoint(x: 57.5, y: 35), control: CGPoint(x: 54, y: 21))
        spike.closeSubpath()
        context.addPath(spike)
        context.fillPath()
        context.restoreGState()
    }
    context.fillEllipse(in: CGRect(x: 54 - 19, y: 54 - 19, width: 38, height: 38))

    // Without a pore colour the pores are cut out, so the system's background shows through.
    if let pores {
        context.setFillColor(pores)
    } else {
        context.setBlendMode(.clear)
    }
    for (x, y) in [(69.0, 54.0), (46.5, 67.0), (46.5, 41.0)] {
        context.fillEllipse(in: CGRect(x: x - 3, y: y - 3, width: 6, height: 6))
    }
    context.setBlendMode(.normal)
}

func render(_ name: String, opaque: Bool, background: CGColor?, glyph: CGColor, pores: CGColor?, into directory: URL) {
    let context = CGContext(
        data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: 0,
        space: CGColorSpace(name: CGColorSpace.sRGB)!,
        bitmapInfo: opaque ? CGImageAlphaInfo.noneSkipLast.rawValue : CGImageAlphaInfo.premultipliedLast.rawValue
    )!
    if let background {
        context.setFillColor(background)
        context.fill(CGRect(x: 0, y: 0, width: size, height: size))
    }
    let scale = Double(size) / viewportSize
    context.translateBy(x: 0, y: Double(size))
    context.scaleBy(x: scale, y: -scale)
    context.translateBy(x: -viewportOrigin, y: -viewportOrigin)
    grain(in: context, glyph: glyph, pores: pores)

    let url = directory.appendingPathComponent(name)
    let destination = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil)!
    CGImageDestinationAddImage(destination, context.makeImage()!, nil)
    guard CGImageDestinationFinalize(destination) else { fatalError("Could not write \(url.path)") }
    print("Wrote \(url.path)")
}

guard CommandLine.arguments.count == 2 else {
    print("usage: swift render-app-icon.swift <AppIcon.appiconset directory>")
    exit(64)
}
let directory = URL(fileURLWithPath: CommandLine.arguments[1], isDirectory: true)
render("AppIcon.png", opaque: true, background: olive, glyph: white, pores: olive, into: directory)
render("AppIcon-Dark.png", opaque: false, background: nil, glyph: paleOlive, pores: nil, into: directory)
render("AppIcon-Tinted.png", opaque: false, background: nil, glyph: white, pores: nil, into: directory)
