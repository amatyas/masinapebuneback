package com.caradchecker.springbatch.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkUtils {

    /**
     * Convertit une IP en entier 32 bits (représentation binaire utilisée en Java)
     */
    private static int ipToInt(String ipAddress) throws UnknownHostException {
        byte[] octets = InetAddress.getByName(ipAddress).getAddress();
        int result = 0;
        for (byte octet : octets) {
            result <<= 8;
            result |= (octet & 0xFF);
        }
        return result;
    }

    /**
     * Convertit un entier 32 bits en format texte (x.x.x.x)
     */
    private static String intToIp(int ip) {
        return ((ip >> 24) & 0xFF) + "." +
               ((ip >> 16) & 0xFF) + "." +
               ((ip >> 8) & 0xFF) + "." +
               (ip & 0xFF);
    }

    /**
     * Calcule le CIDR à partir d'un masque (ex: 255.255.255.0 -> 24)
     */
    private static int getCidrFromMask(String subnetMask) throws UnknownHostException {
        int mask = ipToInt(subnetMask);
        return Integer.bitCount(mask);
    }

    public static String getSubnetNotationFromIPv4AndSubnetMask(String ipAddress, String subnetMask) {
        if (ipAddress == null || ipAddress.isEmpty() || subnetMask == null || subnetMask.isEmpty()) {
            return "";
        }

        try {
            int ipBin = ipToInt(ipAddress);
            int maskBin = ipToInt(subnetMask);

            // Calcul de l'adresse réseau (ET logique)
            int networkAddressBin = ipBin & maskBin;
            
            // Calcul du CIDR
            int cidr = Integer.bitCount(maskBin);

            return intToIp(networkAddressBin) + "/" + cidr;

        } catch (UnknownHostException e) {
            // Log de l'erreur ici selon votre framework (ex: SLF4J)
            System.err.println("Erreur de format IP/Mask: " + e.getMessage());
            return "";
        }
    }
}