package test.switches;

/**
 * A component using switch on integers and enum.
 */
public class Switches {


    public int switchOnInteger(int i) {
        switch (i) {
            case 0 : return 0;
            case 1 : return 1;
            case 2 : return 2;
            default: return 3;
        }
    }

    public int switchOnEnum(Color c) {
        switch (c) {
            case RED: return 1;
            case GREEN: return 2;
            default: return 3;
        }
    }
}
