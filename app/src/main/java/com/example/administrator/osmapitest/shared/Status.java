package com.example.administrator.osmapitest.shared;

public class Status {
    private static boolean alterIndoorMapIsNotExist = true;
    private static boolean still = true;
    private static boolean indoor = false;  // Se o usuário está em ambiente interno ou externo (0 é interno, 1 é externo)

    public static void setAlterIndoorMapIsNotExist(boolean alterIndoorMapIsNotExist) {
        Status.alterIndoorMapIsNotExist = alterIndoorMapIsNotExist;
    }

    public static boolean isAlterIndoorMapIsNotExist() {
        return alterIndoorMapIsNotExist;
    }

    public static boolean isStill() {
        return still;
    }

    public static boolean isIndoor() {
        return indoor;
    }

    public static void setStill(boolean still) {
        Status.still = still;
    }

    public static void setIsIndoor(boolean isIndoor) {
        Status.indoor = isIndoor;
    }
}
