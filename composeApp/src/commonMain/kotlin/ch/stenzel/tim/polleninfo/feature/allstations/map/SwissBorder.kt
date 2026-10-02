package ch.stenzel.tim.polleninfo.feature.allstations.map

/** A WGS84 position in decimal degrees, as the station list and [SWISS_BORDER] carry it. */
data class GeoPoint(val latitude: Double, val longitude: Double)

/**
 * The outer border of Switzerland as one ring of WGS84 points, closed (first point == last).
 *
 * Source: Natural Earth admin-0 countries, 1:10m (`ne_10m_admin_0_countries`, feature `CHE`),
 * public domain — no attribution is required, it is recorded here so the data can be re-derived.
 * The 749-point ring was simplified with Douglas–Peucker at a tolerance of 0.01° (measured with
 * longitude scaled by cos 46.8°, as [SwissMapProjection] draws it) to 203 points and rounded to four
 * decimals — finer than anything visible on a phone-sized map.
 *
 * Enclaves (Büsingen, Campione) are ignored; at this scale they would not be visible.
 */
internal val SWISS_BORDER: List<GeoPoint> = listOf(
    GeoPoint(46.8644, 10.4538), GeoPoint(46.7989, 10.4172), GeoPoint(46.7556, 10.4287),
    GeoPoint(46.6724, 10.3692), GeoPoint(46.6388, 10.3956), GeoPoint(46.6236, 10.4591),
    GeoPoint(46.5785, 10.4659), GeoPoint(46.5377, 10.4440), GeoPoint(46.5511, 10.2954),
    GeoPoint(46.5753, 10.2347), GeoPoint(46.6180, 10.2338), GeoPoint(46.6268, 10.1921),
    GeoPoint(46.6044, 10.0878), GeoPoint(46.5330, 10.0328), GeoPoint(46.4670, 10.0440),
    GeoPoint(46.4463, 10.0263), GeoPoint(46.4029, 10.1408), GeoPoint(46.3381, 10.0924),
    GeoPoint(46.2624, 10.1589), GeoPoint(46.2311, 10.1179), GeoPoint(46.2205, 10.0427),
    GeoPoint(46.2601, 10.0317), GeoPoint(46.2981, 9.9776), GeoPoint(46.3561, 9.9640),
    GeoPoint(46.3712, 9.9184), GeoPoint(46.3386, 9.7681), GeoPoint(46.3509, 9.7201),
    GeoPoint(46.3117, 9.7084), GeoPoint(46.2918, 9.6743), GeoPoint(46.2986, 9.5365),
    GeoPoint(46.3753, 9.4444), GeoPoint(46.4983, 9.4346), GeoPoint(46.4664, 9.3846),
    GeoPoint(46.5015, 9.3310), GeoPoint(46.4851, 9.2632), GeoPoint(46.4365, 9.2380),
    GeoPoint(46.4167, 9.2609), GeoPoint(46.3314, 9.2752), GeoPoint(46.2312, 9.2248),
    GeoPoint(46.1723, 9.1632), GeoPoint(46.1382, 9.0906), GeoPoint(46.0618, 9.0592),
    GeoPoint(46.0393, 9.0021), GeoPoint(45.9931, 9.0156), GeoPoint(45.9644, 8.9805),
    GeoPoint(45.8990, 9.0631), GeoPoint(45.8207, 9.0024), GeoPoint(45.8348, 8.9396),
    GeoPoint(45.8264, 8.9000), GeoPoint(45.8834, 8.9121), GeoPoint(45.9471, 8.8710),
    GeoPoint(45.9831, 8.7679), GeoPoint(46.0664, 8.8344), GeoPoint(46.0897, 8.8089),
    GeoPoint(46.1095, 8.7239), GeoPoint(46.0958, 8.6775), GeoPoint(46.1228, 8.6018),
    GeoPoint(46.2079, 8.5103), GeoPoint(46.2354, 8.4381), GeoPoint(46.2758, 8.4232),
    GeoPoint(46.4124, 8.4458), GeoPoint(46.4487, 8.4279), GeoPoint(46.4337, 8.3163),
    GeoPoint(46.3701, 8.2815), GeoPoint(46.3092, 8.1926), GeoPoint(46.2718, 8.0873),
    GeoPoint(46.2536, 8.0731), GeoPoint(46.1960, 8.1295), GeoPoint(46.1270, 8.1106),
    GeoPoint(46.0911, 8.0253), GeoPoint(45.9993, 7.9858), GeoPoint(45.9819, 7.8982),
    GeoPoint(45.9145, 7.8312), GeoPoint(45.9287, 7.6940), GeoPoint(45.9663, 7.6430),
    GeoPoint(45.9841, 7.5411), GeoPoint(45.9078, 7.3618), GeoPoint(45.9134, 7.2867),
    GeoPoint(45.8765, 7.1535), GeoPoint(45.8902, 7.0669), GeoPoint(45.9333, 7.0152),
    GeoPoint(45.9931, 6.9877), GeoPoint(46.0486, 6.9151), GeoPoint(46.0496, 6.8509),
    GeoPoint(46.1123, 6.8692), GeoPoint(46.1348, 6.7743), GeoPoint(46.1859, 6.7749),
    GeoPoint(46.2695, 6.8277), GeoPoint(46.3455, 6.7504), GeoPoint(46.3952, 6.7892),
    GeoPoint(46.4293, 6.7627), GeoPoint(46.4574, 6.5470), GeoPoint(46.4486, 6.4829),
    GeoPoint(46.4082, 6.3977), GeoPoint(46.3945, 6.3016), GeoPoint(46.3155, 6.2141),
    GeoPoint(46.2679, 6.2376), GeoPoint(46.2631, 6.2760), GeoPoint(46.2401, 6.2812),
    GeoPoint(46.1386, 6.1079), GeoPoint(46.1479, 6.0283), GeoPoint(46.1305, 5.9588),
    GeoPoint(46.1708, 5.9829), GeoPoint(46.2120, 5.9585), GeoPoint(46.2464, 6.0897),
    GeoPoint(46.3704, 6.1351), GeoPoint(46.4194, 6.0542), GeoPoint(46.4711, 6.0642),
    GeoPoint(46.5516, 6.1457), GeoPoint(46.5835, 6.1184), GeoPoint(46.7608, 6.4292),
    GeoPoint(46.8022, 6.4171), GeoPoint(46.8577, 6.4468), GeoPoint(46.9091, 6.4278),
    GeoPoint(46.9442, 6.4426), GeoPoint(46.9865, 6.5987), GeoPoint(47.0213, 6.6654),
    GeoPoint(47.0783, 6.6897), GeoPoint(47.2452, 6.9562), GeoPoint(47.2906, 6.9586),
    GeoPoint(47.3295, 7.0366), GeoPoint(47.3405, 7.0443), GeoPoint(47.3681, 7.0040),
    GeoPoint(47.3542, 6.8666), GeoPoint(47.4249, 6.9261), GeoPoint(47.4522, 6.9910),
    GeoPoint(47.4891, 6.9733), GeoPoint(47.4992, 7.0098), GeoPoint(47.4883, 7.1808),
    GeoPoint(47.4436, 7.1683), GeoPoint(47.4168, 7.2381), GeoPoint(47.4382, 7.4063),
    GeoPoint(47.4651, 7.4293), GeoPoint(47.4902, 7.4143), GeoPoint(47.4819, 7.4674),
    GeoPoint(47.4929, 7.4845), GeoPoint(47.5149, 7.4769), GeoPoint(47.5330, 7.5052),
    GeoPoint(47.5423, 7.4827), GeoPoint(47.5950, 7.6370), GeoPoint(47.5966, 7.6597),
    GeoPoint(47.5715, 7.6465), GeoPoint(47.5647, 7.6097), GeoPoint(47.5443, 7.6834),
    GeoPoint(47.5560, 7.7667), GeoPoint(47.5953, 7.8197), GeoPoint(47.5878, 7.8982),
    GeoPoint(47.5606, 7.9122), GeoPoint(47.5606, 8.0423), GeoPoint(47.6158, 8.1790),
    GeoPoint(47.6220, 8.2330), GeoPoint(47.6158, 8.2886), GeoPoint(47.5922, 8.3063),
    GeoPoint(47.5810, 8.3541), GeoPoint(47.5843, 8.4489), GeoPoint(47.6061, 8.4618),
    GeoPoint(47.6219, 8.5224), GeoPoint(47.5894, 8.5607), GeoPoint(47.6563, 8.6073),
    GeoPoint(47.6629, 8.5682), GeoPoint(47.6399, 8.4583), GeoPoint(47.6655, 8.3913),
    GeoPoint(47.6995, 8.3921), GeoPoint(47.7232, 8.4379), GeoPoint(47.7639, 8.4636),
    GeoPoint(47.7793, 8.5516), GeoPoint(47.8012, 8.5582), GeoPoint(47.7946, 8.6016),
    GeoPoint(47.7573, 8.6174), GeoPoint(47.7910, 8.6441), GeoPoint(47.7587, 8.6819),
    GeoPoint(47.7574, 8.7131), GeoPoint(47.7235, 8.7004), GeoPoint(47.6946, 8.7171),
    GeoPoint(47.6951, 8.7699), GeoPoint(47.7209, 8.7707), GeoPoint(47.7200, 8.7976),
    GeoPoint(47.6907, 8.8561), GeoPoint(47.6808, 8.8378), GeoPoint(47.6561, 8.8817),
    GeoPoint(47.6543, 8.9454), GeoPoint(47.6789, 9.0166), GeoPoint(47.6704, 9.1834),
    GeoPoint(47.6561, 9.1969), GeoPoint(47.6501, 9.2732), GeoPoint(47.5345, 9.5475),
    GeoPoint(47.4807, 9.5845), GeoPoint(47.4521, 9.6503), GeoPoint(47.3945, 9.6398),
    GeoPoint(47.2100, 9.4874), GeoPoint(47.1080, 9.5124), GeoPoint(47.0639, 9.4770),
    GeoPoint(47.0562, 9.6691), GeoPoint(47.0155, 9.8580), GeoPoint(46.9274, 9.8751),
    GeoPoint(46.8471, 10.1113), GeoPoint(46.8668, 10.2014), GeoPoint(46.9233, 10.2351),
    GeoPoint(46.9227, 10.2957), GeoPoint(46.9643, 10.3137), GeoPoint(46.9955, 10.3790),
    GeoPoint(46.9366, 10.4585), GeoPoint(46.8644, 10.4538),
)
