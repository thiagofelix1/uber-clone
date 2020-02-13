package com.example.thiagofelix.uber.helper;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;

public class Local {
    public static float calcularDistancia(LatLng latLngInicial, LatLng latLngFinal){
        Location localInicial = new Location("local inicial");
        localInicial.setLatitude(latLngInicial.latitude);
        localInicial.setLongitude(latLngInicial.longitude);

        Location localFinal = new Location("local final");
        localFinal.setLatitude(latLngFinal.latitude);
        localFinal.setLongitude(latLngFinal.longitude);

        //calcula distancia e retorna em metros
        //dividir por 1000 para ter o resultado em KM
        float distancia  = (localInicial.distanceTo(localFinal))/1000;

        return distancia;
    }

    public static String formatarDistancia(float distancia){
        String distanciaFormatada;
        if(distancia<1){
            distancia = distancia*1000;
            distanciaFormatada = Math.round(distancia)+"M";

        }else{
            DecimalFormat decimal = new DecimalFormat("0.00");
            distanciaFormatada = decimal.format(distancia+" KM");
        }
        return distanciaFormatada;
    }

}
