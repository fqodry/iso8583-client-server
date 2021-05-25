package id.mbingweb.iso8583;

import id.mbingweb.iso8583.helper.DecimalHexBinaryConverter;

/**
 *
 * @author fqodry
 */
public class Main {
    public static void main(String[] args) {
        int decimalNum = 20210525;
        
        System.out.println(DecimalHexBinaryConverter.decimalToHexa(decimalNum));
    }
}
