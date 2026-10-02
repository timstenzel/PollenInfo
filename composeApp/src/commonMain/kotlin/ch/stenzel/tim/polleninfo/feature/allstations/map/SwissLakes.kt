package ch.stenzel.tim.polleninfo.feature.allstations.map

/** One lake as a closed ring of WGS84 points (first point == last). [name] is for reading the data. */
internal data class Lake(val name: String, val outline: List<GeoPoint>)

/**
 * The big lakes in and on the border of Switzerland, drawn on the map as landmarks so the station
 * dots are easier to place — Genève at the end of Lac Léman, Zürich at the end of the Zürichsee.
 *
 * Source: Natural Earth lakes, 1:10m (public domain) — `ne_10m_lakes` for Lac Léman,
 * `ne_10m_lakes_europe` for the rest. Outer rings only, simplified with Douglas–Peucker at 0.003°
 * (longitude scaled by cos 46.8°, as [SwissMapProjection] draws it) and rounded to four decimals.
 *
 * Border lakes are kept whole rather than cut at the border: half a Léman is harder to recognise
 * than the whole one. The Bodensee and Lago Maggiore are left out on purpose.
 */
internal val SWISS_LAKES: List<Lake> = listOf(
    Lake(
        "Lac Léman",
        listOf(
            GeoPoint(46.4587, 6.8712), GeoPoint(46.4419, 6.9165), GeoPoint(46.4231, 6.9354),
            GeoPoint(46.4043, 6.9139), GeoPoint(46.4025, 6.8677), GeoPoint(46.4231, 6.6972),
            GeoPoint(46.4093, 6.4994), GeoPoint(46.3890, 6.4791), GeoPoint(46.3654, 6.4082),
            GeoPoint(46.3480, 6.3837), GeoPoint(46.3716, 6.3555), GeoPoint(46.3739, 6.2948),
            GeoPoint(46.3124, 6.2371), GeoPoint(46.2388, 6.1830), GeoPoint(46.2047, 6.1332),
            GeoPoint(46.2312, 6.1532), GeoPoint(46.2689, 6.1600), GeoPoint(46.3001, 6.1765),
            GeoPoint(46.4511, 6.3184), GeoPoint(46.4673, 6.3528), GeoPoint(46.4731, 6.4191),
            GeoPoint(46.5176, 6.5109), GeoPoint(46.5223, 6.5977), GeoPoint(46.5194, 6.6201),
            GeoPoint(46.4587, 6.8712),
        ),
    ),
    Lake(
        "Lac de Neuchâtel",
        listOf(
            GeoPoint(46.8914, 6.8867), GeoPoint(46.8652, 6.8533), GeoPoint(46.8130, 6.7496),
            GeoPoint(46.8151, 6.7305), GeoPoint(46.7941, 6.6597), GeoPoint(46.8018, 6.6436),
            GeoPoint(46.8173, 6.6472), GeoPoint(46.8644, 6.7341), GeoPoint(46.9127, 6.7925),
            GeoPoint(46.9340, 6.8229), GeoPoint(46.9482, 6.8557), GeoPoint(46.9826, 6.8800),
            GeoPoint(47.0197, 6.9849), GeoPoint(47.0023, 7.0435), GeoPoint(46.9802, 7.0350),
            GeoPoint(46.8914, 6.8867),
        ),
    ),
    Lake(
        "Bielersee",
        listOf(
            GeoPoint(47.0552, 7.0916), GeoPoint(47.0598, 7.0767), GeoPoint(47.0695, 7.0913),
            GeoPoint(47.1406, 7.2258), GeoPoint(47.1388, 7.2332), GeoPoint(47.1303, 7.2283),
            GeoPoint(47.0774, 7.1870), GeoPoint(47.0552, 7.1533), GeoPoint(47.0473, 7.1226),
            GeoPoint(47.0552, 7.0916),
        ),
    ),
    Lake(
        "Thunersee",
        listOf(
            GeoPoint(46.6722, 7.7344), GeoPoint(46.7108, 7.6565), GeoPoint(46.7285, 7.6372),
            GeoPoint(46.7428, 7.6332), GeoPoint(46.7525, 7.6450), GeoPoint(46.7212, 7.7073),
            GeoPoint(46.7033, 7.7393), GeoPoint(46.6914, 7.7776), GeoPoint(46.6865, 7.8164),
            GeoPoint(46.6741, 7.8223), GeoPoint(46.6644, 7.7769), GeoPoint(46.6722, 7.7344),
        ),
    ),
    Lake(
        "Brienzersee",
        listOf(
            GeoPoint(46.6984, 7.9046), GeoPoint(46.7063, 7.8941), GeoPoint(46.7583, 7.9841),
            GeoPoint(46.7653, 8.0055), GeoPoint(46.7650, 8.0305), GeoPoint(46.7555, 8.0483),
            GeoPoint(46.7467, 8.0377), GeoPoint(46.7398, 8.0106), GeoPoint(46.7094, 7.9441),
            GeoPoint(46.6984, 7.9046),
        ),
    ),
    Lake(
        "Vierwaldstättersee",
        listOf(
            GeoPoint(47.0488, 8.3533), GeoPoint(47.0576, 8.3860), GeoPoint(47.0743, 8.4210),
            GeoPoint(47.0677, 8.4304), GeoPoint(47.0494, 8.4131), GeoPoint(47.0385, 8.4237),
            GeoPoint(47.0300, 8.4653), GeoPoint(47.0009, 8.5076), GeoPoint(46.9999, 8.5374),
            GeoPoint(47.0090, 8.5806), GeoPoint(47.0060, 8.5991), GeoPoint(46.9780, 8.6155),
            GeoPoint(46.9352, 8.6171), GeoPoint(46.9215, 8.6246), GeoPoint(46.9050, 8.6167),
            GeoPoint(46.8999, 8.6006), GeoPoint(46.9327, 8.5879), GeoPoint(46.9951, 8.5912),
            GeoPoint(46.9764, 8.5316), GeoPoint(46.9716, 8.4937), GeoPoint(46.9817, 8.4465),
            GeoPoint(46.9899, 8.4221), GeoPoint(46.9999, 8.4390), GeoPoint(47.0098, 8.4147),
            GeoPoint(47.0017, 8.3599), GeoPoint(46.9707, 8.3215), GeoPoint(46.9616, 8.2942),
            GeoPoint(46.9647, 8.2830), GeoPoint(46.9944, 8.3150), GeoPoint(47.0181, 8.3128),
            GeoPoint(47.0115, 8.3352), GeoPoint(47.0275, 8.3392), GeoPoint(47.0580, 8.3307),
            GeoPoint(47.0595, 8.3384), GeoPoint(47.0488, 8.3533),
        ),
    ),
    Lake(
        "Zugersee",
        listOf(
            GeoPoint(47.0756, 8.5071), GeoPoint(47.0941, 8.4794), GeoPoint(47.1233, 8.4761),
            GeoPoint(47.1561, 8.4590), GeoPoint(47.1868, 8.4620), GeoPoint(47.1851, 8.4924),
            GeoPoint(47.1610, 8.5103), GeoPoint(47.1254, 8.4995), GeoPoint(47.0886, 8.5269),
            GeoPoint(47.0741, 8.5241), GeoPoint(47.0756, 8.5071),
        ),
    ),
    Lake(
        "Zürichsee",
        listOf(
            GeoPoint(47.2461, 8.7232), GeoPoint(47.2424, 8.7615), GeoPoint(47.2458, 8.8064),
            GeoPoint(47.2275, 8.8325), GeoPoint(47.2282, 8.9370), GeoPoint(47.2175, 8.9221),
            GeoPoint(47.2136, 8.8731), GeoPoint(47.2020, 8.8338), GeoPoint(47.2154, 8.7688),
            GeoPoint(47.2120, 8.7310), GeoPoint(47.2178, 8.7085), GeoPoint(47.2461, 8.6658),
            GeoPoint(47.2765, 8.5903), GeoPoint(47.3187, 8.5581), GeoPoint(47.3607, 8.5371),
            GeoPoint(47.3619, 8.5523), GeoPoint(47.3038, 8.5982), GeoPoint(47.2747, 8.6526),
            GeoPoint(47.2461, 8.7232),
        ),
    ),
    Lake(
        "Walensee",
        listOf(
            GeoPoint(47.1224, 9.2061), GeoPoint(47.1327, 9.1380), GeoPoint(47.1406, 9.1314),
            GeoPoint(47.1446, 9.1504), GeoPoint(47.1394, 9.2322), GeoPoint(47.1306, 9.3021),
            GeoPoint(47.1239, 9.2800), GeoPoint(47.1224, 9.2061),
        ),
    ),
    Lake(
        "Lago di Lugano",
        listOf(
            GeoPoint(45.9528, 8.9656), GeoPoint(45.9215, 8.9830), GeoPoint(45.9143, 8.9773),
            GeoPoint(45.9388, 8.9602), GeoPoint(45.9127, 8.9015), GeoPoint(45.9744, 8.8860),
            GeoPoint(45.9993, 8.9033), GeoPoint(45.9501, 8.9027), GeoPoint(45.9364, 8.9088),
            GeoPoint(45.9364, 8.9249), GeoPoint(45.9750, 8.9517), GeoPoint(46.0124, 8.9605),
            GeoPoint(46.0109, 8.9898), GeoPoint(46.0233, 9.0216), GeoPoint(46.0413, 9.1218),
            GeoPoint(46.0264, 9.1106), GeoPoint(46.0170, 9.0393), GeoPoint(46.0033, 9.0071),
            GeoPoint(45.9750, 8.9714), GeoPoint(45.9528, 8.9656),
        ),
    ),
)
