package com.example.administrator.osmapitest.shared;

import com.example.administrator.osmapitest.data.ClientPos;

/**
 * A classe NowClientPos é uma classe pública
 * Que armazena as variáveis ​​estáticas nowLongitude, nowLatitude e isIndoor
 * Representa a latitude e longitude atuais e o status de localização do cliente
 * Usado para compartilhamento de dados entre programas
 */
public class NowClientPos {
    private static double nowLongitude;    // 用户当前经度
    private static double nowLatitude; // 用户当前纬度
    private static int nowFloor;   // 楼层


    /**
     * Obtenha a latitude atual do usuário
     *
     * @return latitude atual
     */
    public static double getNowLatitude() {
        return nowLatitude;
    }

    /**
     * Obtenha a longitude atual do usuário
     *
     * @return longitude atual
     */
    public static double getNowLongitude() {
        return nowLongitude;
    }


    /**
     * Obtenha o andar atual do usuário
     *
     * @return andar do usuário
     */
    public static int getNowFloor() {
        return nowFloor;
    }

    /**
     * Definir a longitude atual do usuário
     *
     * @param nowLongitude <Longitude atual do usuário>
     */
    public static void setNowLongitude(double nowLongitude) {
        NowClientPos.nowLongitude = nowLongitude;
    }

    /**
     * Definir a latitude atual do usuário
     *
     * @param nowLatitude Latitude atual do usuário
     */
    public static void setNowLatitude(double nowLatitude) {
        NowClientPos.nowLatitude = nowLatitude;
    }


    /**
     * definir piso
     *
     * @param nowFloor piso
     */
    public static void setNowFloor(int nowFloor) {
        NowClientPos.nowFloor = nowFloor;
    }

    /**
     * Definir parâmetros posicionais
     *
     * @param ClientPos localização do usuário
     */
    public static void setPosPara(ClientPos ClientPos) {
        NowClientPos.nowLongitude = ClientPos.getLongitude();
        NowClientPos.nowLatitude = ClientPos.getLatitude();
        NowClientPos.nowFloor = ClientPos.getFloor();
    }
}
