package burp;

public class IPAddress {

    public static boolean isPrivateIPv4(String ipAddress) {
        try {
            String[] ipAddressArray = ipAddress.split("\\.");
            int[] ipParts = new int[ipAddressArray.length];
            for (int i = 0; i < ipAddressArray.length; i++) {
                ipParts[i] = Integer.parseInt(ipAddressArray[i].trim());
            }
            
            switch (ipParts[0]) {
                case 10:
                case 127:
                    return true;
                case 172:
                    return (ipParts[1] >= 16) && (ipParts[1] < 32);
                case 192:
                    return (ipParts[1] == 168);
                case 169:
                    return (ipParts[1] == 254);
            }
        } catch (Exception ex) {
        }
        
        return false;
    }

    public static boolean isPrivateIPv6(String ipAddress) {
        boolean isPrivateIPv6 = false;
        String[] ipParts = ipAddress.trim().split(":");
        if (ipParts.length > 0) {
            String firstBlock = ipParts[0];
            String prefix = firstBlock.substring(0, 2);
            
            if (firstBlock.equalsIgnoreCase("fe80")
                    || firstBlock.equalsIgnoreCase("100")
                    || ((prefix.equalsIgnoreCase("fc") && firstBlock.length() >= 4))
                    || ((prefix.equalsIgnoreCase("fd") && firstBlock.length() >= 4))) {
                isPrivateIPv6 = true;
            }
        }
        return isPrivateIPv6;
    }
    
    
    public static void main(String[] args) {
        // test IPv4
        String ipv4Address = "127.56.87.4";

        if (IPAddress.isPrivateIPv4(ipv4Address)) {
            System.out.println("This is a private IPv4");
        }
        
        
        // test IPv6 
        String ipv6Address = "fe80:db8:a0b:12f0::1";

        if (IPAddress.isPrivateIPv6(ipv6Address)) {
            System.out.println("This is a private IPv6");
        }
    }
}