
attribute vec4 a_Position;
attribute vec2 a_TexCoordinate;

//uniform mat4 u_Matrix;

varying vec2 v_TexCoordinate;

void main() {
    v_TexCoordinate = a_TexCoordinate;
    //gl_Position = u_Matrix * a_Position;
    gl_Position = a_Position;
}
