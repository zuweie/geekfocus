package oz.geekfocus.me;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class util {
	public static String md5(String text) throws NoSuchAlgorithmException{

		MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(text.getBytes());
        byte b[] = md.digest();
        int i;
        StringBuffer buf = new StringBuffer("");
        for (int ii=0; ii<b.length; ++ii){
       	 i = b[ii];
       	 if (i < 0)
       		 i += 256;
       	 if (i < 16)
       		 buf.append("0");
       	 buf.append(Integer.toHexString(i));
        }
		return buf.toString();
	
	}
}
