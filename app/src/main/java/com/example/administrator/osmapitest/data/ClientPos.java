package com.example.administrator.osmapitest.data;

import java.io.Serializable;

/**
 * Latitude e longitude do cliente
 */
public class ClientPos implements Serializable {
    private double longitude;    // Longitude atual do usuário
    private double latitude;    // Latitude atual do usuário
    private int floor;          // O piso atual, se for exterior, é no piso 0
    private float accuracy;     // Confiança da posição atual
    private String provider;

    public ClientPos(double latitude, double longitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public ClientPos(double latitude, double longitude, int floor) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.floor = floor;
    }

    public ClientPos(String[] posStr) {
        this.longitude = Double.valueOf(posStr[1]);
        this.latitude = Double.valueOf(posStr[0]);
    }

    public int getFloor() {
        return floor;
    }

    /**
     * Obtenha a latitude atual do usuário
     *
     * @return latitude atual
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Obtenha a longitude atual do usuário
     *
     * @return longitude atual
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Obter a precisão da localização atual
     *
     * @return precisao
     */
    public float getAccuracy() {
        return accuracy;
    }

    /**
     * Obtenha o método de aquisição de localização atual
     *
     * @return (GPS or Network)
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Defina a precisão da posição atual
     *
     * @param accuracy
     */
    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    /**
     * Defina o caminho para obter a localização atual
     *
     * @param provider <método de aquisição de localização>
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * definir piso atual
     *
     * @param floor <floor>
     */
    public void setFloor(int floor) {
        this.floor = floor;
    }
}
