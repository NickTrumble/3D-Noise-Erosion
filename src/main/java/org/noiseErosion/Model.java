package org.noiseErosion;


//TODO:
//add in more shapes based on internal angles, e.g. 3d hex
//holds 3d object (model)(square for now) to get eroded
public class Model {
    private Vector3[] vertices;
    private int[][] triangles;
    private final float edgeLength;
    private final Vector3 offset;

    public Model(float edgeLength, Vector3 offset){
        this.edgeLength = edgeLength;
        this.offset = offset;
        populateVerticeArray();
        populateTriangleArray();
    }

    private void populateVerticeArray(){
        int count = 0;
        vertices = new Vector3[8];
        for (int i = -1; i <= 1; i+= 2) {
            for(int j = -1; j <= 1; j+= 2){
                for (int k = -1; k <= 1; k+= 2){
                    Vector3 v = new Vector3(i, j, k);
                    v.multiply(edgeLength);
                    v.add(offset);
                    vertices[count++] = v;
                }
            }
        }
    }

    private void populateTriangleArray(){
        triangles = new int[12][3];

        // back face -z
        triangles[0] = new int[]{0, 4, 6};
        triangles[1] = new int[]{0, 6, 2};

        // front face +z
        triangles[2] = new int[]{1, 3, 7};
        triangles[3] = new int[]{1, 7, 5};

        // left face -x
        triangles[4] = new int[]{0, 2, 3};
        triangles[5] = new int[]{0, 3, 1};

        // right face +x
        triangles[6] = new int[]{4, 5, 7};
        triangles[7] = new int[]{4, 7, 6};

        // bottom face -y
        triangles[8] = new int[]{0, 1, 5};
        triangles[9] = new int[]{0, 5, 4};

        // top face +y
        triangles[10] = new int[]{2, 6, 7};
        triangles[11] = new int[]{2, 7, 3};
    }

    public int[][] getTriangles() { return triangles; }
    public Vector3[] getVertices() { return vertices; }
}
