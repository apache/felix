package test;

/**
 * Checks a component with a double array
 * To reproduce https://issues.apache.org/jira/browse/FELIX-3621
 */
public class DoubleArray {

    public boolean start() {
        System.out.println("Start");

        int[][] matrix = new int[20][20];

        this.testArray(matrix);

        matrix[0][0] = 2;
        return true;
    }

    private void testArray(int[][] matrix) {
        System.out.println("Test Array");
    }


}
