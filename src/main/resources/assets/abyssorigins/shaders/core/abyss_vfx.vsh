#version 150

// Shared vertex stage for every AbyssOrigins VFX material.
// Positions arrive camera-relative, so their length is the fog distance.

in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

out vec2 texCoord0;
out vec4 vertexColor;
out float vertexDistance;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord0 = UV0;
    vertexColor = Color;
    vertexDistance = FogShape == 0 ? length(Position) : max(length(Position.xz), abs(Position.y));
}
