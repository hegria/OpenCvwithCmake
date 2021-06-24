package com.example.opencvwithcmake;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Cube {
  int vBuffer, iBuffer;
  int iLength;

  int program;

  int width, height;

  private final String vscode = "" +
          "attribute vec3 vPosition;" +
          "" +
          "uniform mat4 modelMX;" +
          "uniform mat4 viewMX;" +
          "uniform mat4 projMX;" +
          "" +
          "varying vec3 color;" +
          "" +
          "void main() {" +
          "  gl_Position = projMX * viewMX * modelMX * vec4(vPosition, 1.0);" +
          "  color = vPosition;" +
          "}";

  private final String fscode = "" +
          "precision mediump float;" +
          "" +
          "varying vec3 color;" +
          "" +
          "void main() {" +
          "  gl_FragColor = vec4(color, 1.0);" +
          "}";

  public Cube() {
    float[] vertices = {
            -1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, 1.0f, -1.0f,
            -1.0f, 1.0f, -1.0f,

            -1.0f, -1.0f, 1.0f,
            1.0f, -1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, 1.0f,
    };
    short[] indices = {
            0, 1, 3, 3, 1, 2,
            0, 1, 4, 4, 5, 1,
            1, 2, 5, 5, 6, 2,
            2, 3, 6, 6, 7, 3,
            3, 7, 4, 4, 3, 0,
            4, 5, 7, 7, 6, 5
    };

    iLength = indices.length;

    ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
    bb.order(ByteOrder.nativeOrder());
    FloatBuffer vb = bb.asFloatBuffer();
    vb.put(vertices);
    vb.position(0);

    ByteBuffer bb2 = ByteBuffer.allocateDirect(indices.length * 2);
    bb2.order(ByteOrder.nativeOrder());
    ShortBuffer ib = bb2.asShortBuffer();
    ib.put(indices);
    ib.position(0);

    int[] buffers = new int[2];
    GLES20.glGenBuffers(2, buffers, 0);

    vBuffer = buffers[0];
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vBuffer);
    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.length * 4, vb, GLES20.GL_DYNAMIC_DRAW);
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

    iBuffer = buffers[1];
    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, iBuffer);
    GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indices.length * 2, ib, GLES20.GL_DYNAMIC_DRAW);
    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

    int vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
    GLES20.glShaderSource(vs, vscode);
    GLES20.glCompileShader(vs);
    int fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
    GLES20.glShaderSource(fs, fscode);
    GLES20.glCompileShader(fs);

    program = GLES20.glCreateProgram();
    GLES20.glAttachShader(program, vs);
    GLES20.glAttachShader(program, fs);
    GLES20.glLinkProgram(program);
  }

  float time = 0.0f;

  public void draw(float dt) {
    time += dt;

    GLES20.glUseProgram(program);

    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vBuffer);
    int vpos = GLES20.glGetAttribLocation(program, "vPosition");
    GLES20.glEnableVertexAttribArray(vpos);
    GLES20.glVertexAttribPointer(vpos, 3, GLES20.GL_FLOAT, false, 3 * 4, 0);
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

    float[] scaleMX = {
            0.5f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.5f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.5f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };

    float[] rotateMX = {
            (float) Math.cos(time), (float) Math.sin(time), 0.0f, 0.0f,
            -(float) Math.sin(time), (float) Math.cos(time), 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };

    float[] transMX = {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            (float) Math.cos(time), (float) Math.sin(time), -3.0f, 1.0f
    };

    float[] modelMX = new float[16];
    Matrix.multiplyMM(modelMX, 0, rotateMX, 0, scaleMX, 0);
    Matrix.multiplyMM(modelMX, 0, transMX, 0, modelMX, 0);
    int pos = GLES20.glGetUniformLocation(program, "modelMX");
    GLES20.glUniformMatrix4fv(pos, 1, false, modelMX, 0);

    float eyeX = 0.0f, eyeY = 0.0f, eyeZ = 3.0f;
    float[] viewMX = new float[16];
    Matrix.setLookAtM(viewMX, 0, eyeX, eyeY, eyeZ, eyeX, eyeY, eyeZ - 1.0f, 0.0f, 1.0f, 0.0f);
    pos = GLES20.glGetUniformLocation(program, "viewMX");
    GLES20.glUniformMatrix4fv(pos, 1, false, viewMX, 0);


    float ratio = (float) width / (float) height;
    float[] projMX = new float[16];
    Matrix.frustumM(projMX, 0, -ratio, ratio, -1.0f, 1.0f, 1.0f, 100.0f);
    pos = GLES20.glGetUniformLocation(program, "projMX");
    GLES20.glUniformMatrix4fv(pos, 1, false, projMX, 0);


    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, iBuffer);
    GLES20.glDrawElements(GLES20.GL_TRIANGLES, iLength, GLES20.GL_UNSIGNED_SHORT, 0);
    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

    GLES20.glDisableVertexAttribArray(vpos);
  }

  public void changeSize(int width, int height) {
    this.width = width;
    this.height = height;
  }

}
