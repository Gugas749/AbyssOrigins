#version 150

// "Void" material — matter that eats light (rift cores, shadow tendrils, bat wings, smoke).
// Rendered with normal translucency: deep near-black swirls with veins of the vertex colour.
//   UV.y : 0..1 across; the rim (edges) glows with the vertex colour, the interior is dark.

uniform vec4 ColorModulator;
uniform float GameTime;
uniform float FogStart;
uniform float FogEnd;

in vec2 texCoord0;
in vec4 vertexColor;
in float vertexDistance;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p, float period) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float x0 = mod(i.x, period), x1 = mod(i.x + 1.0, period);
    return mix(mix(hash(vec2(x0, i.y)), hash(vec2(x1, i.y)), u.x),
               mix(hash(vec2(x0, i.y + 1.0)), hash(vec2(x1, i.y + 1.0)), u.x), u.y);
}

float fbm(vec2 p, float period) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * noise(p, period);
        p = p * 2.0 + vec2(0.0, 7.77);
        period *= 2.0;
        a *= 0.5;
    }
    return v;
}

float fogFade(float d) {
    if (d <= FogStart) return 1.0;
    if (d >= FogEnd) return 0.0;
    return smoothstep(FogEnd, FogStart, d);
}

void main() {
    float t = GameTime * 1200.0;
    vec2 uv = texCoord0;
    float across = abs(uv.y * 2.0 - 1.0);

    // Domain-warped swirl
    vec2 q = vec2(fbm(vec2(uv.x * 1.5 + t * 0.3, uv.y * 2.0), 8.0),
                  fbm(vec2(uv.x * 1.5 - t * 0.2, uv.y * 2.0 + 4.0), 8.0));
    float n = fbm(vec2(uv.x * 2.0 + q.x * 2.0 - t * 0.5, uv.y * 3.0 + q.y * 2.0), 16.0);

    float veins = pow(smoothstep(0.45, 0.9, n), 2.0);
    float rim = smoothstep(0.55, 1.0, across + (n - 0.5) * 0.6);

    vec3 dark = vec3(0.012, 0.006, 0.025);
    vec3 col = mix(dark, vertexColor.rgb, clamp(veins * 0.9 + rim * 1.1, 0.0, 1.0));

    float edgeCut = smoothstep(1.0, 0.82, across + (n - 0.5) * 0.5);
    float a = vertexColor.a * edgeCut * (0.78 + 0.22 * n) * fogFade(vertexDistance);
    fragColor = vec4(col, a) * ColorModulator;
}
