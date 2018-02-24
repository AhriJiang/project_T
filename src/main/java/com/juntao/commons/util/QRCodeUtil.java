package com.juntao.commons.util;

import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;
import sun.misc.BASE64Encoder;

/**
 * Created by major on 2017/9/25.
 */
public final class QRCodeUtil {

    public static final String genQRCode(String s) {
        return "data:image/png;base64," + new BASE64Encoder()
                .encode(QRCode.from(s).to(ImageType.PNG).withSize(150, 150).stream().toByteArray());
    }
}
