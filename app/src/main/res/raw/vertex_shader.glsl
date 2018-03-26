attribute vec4 a_Position;
attribute vec2 a_TexCoord;
uniform mat4 u_ProjM;
uniform mat4 u_ModelM;

varying vec2 texCoord;

void main()
{
    mat4 mvp = u_ProjM * u_ModelM;
    gl_Position = mvp * a_Position;
    texCoord = a_TexCoord;
}