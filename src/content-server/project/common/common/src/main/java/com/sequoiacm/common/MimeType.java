package com.sequoiacm.common;

import java.util.HashMap;
import java.util.Map;

public enum MimeType {

    INTERNET_PROPERTY_STREAM("application/internet-property-stream", "acx"),
    POSTSCRIPT("application/postscript", "ai,eps,ps"),
    BASIC("audio/basic", "au,snd"),
    OLESCRIPT("application/olescript", "axs"),
    PLAIN("text/plain", "bas,c,cpp,h,hpp,txt"),
    OCTET_STREAM("application/octet-stream", "bin,cab,chm，class,dms,exe,lha,lzh,msi,ocx,rar"),
    BMP("image/bmp", "bmp"),
    CIS_COD("image/cis-cod", "cod"),
    PKIX_CRL("application/pkix-crl", "crl"),
    CSS("text/css", "css"),
    MSWORD("application/msword", "doc,dot"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
    ENVOY("application/envoy", "evy"),
    FRACTALS("application/fractals", "fif"),
    GIF("image/gif", "gif"),
    WINHLP("application/winhlp", "hlp"),
    MAC_BINHEX40("application/mac-binhex40", "hqx"),
    HTA("application/hta", "hta"),
    HTML("text/html", "htm,html,stm"),
    WEBVIEWHTML("text/webviewhtml", "htt"),
    IEF("image/ief", "ief"),
    PIPEG("image/pipeg", "jfif"),
    JPEG("image/jpeg", "jpe,jpeg,jpg"),

    RFC822("message/rfc822", "mht,mhtml,nws"),
    QUICKTIME("video/quicktime", "mov,qt"),

    AUDIO_MPEG("audio/mpeg", "mp3"),
    VIDEO_MPEG("video/mpeg", "mp2,mpe,mpeg,mpg,mpv2"),

    MS_PROJECT("application/vnd.ms-project", "mpp"),
    MS_PKISECCAT("application/vnd.ms-pkiseccat", "cat"),
    MS_PKIPKO("application/ynd.ms-pkipko", "pko"),
    MS_POWERPOINT("application/vnd.ms-powerpoint", "pot,pps,ppt"),
    MS_PKICERTSTORE("application/vnd.ms-pkicertstore", "sst"),
    MS_PKISTL("application/vnd.ms-pkistl", "stl"),
    MS_WORKS("application/vnd.ms-works", "wcm,wdb,wks,wps"),
    MS_EXCEL("application/vnd.ms-excel", "xla,xlc,xlm,xls,xlt,xlw"),

    ODA("application/oda", "oda"),
    PKCS10("application/pkcs10", "p10"),
    X_VRML("x-world/x-vrml", "flr,vrml,wrl,wrz,xaf,xof"),
    X_GTAR("application/x-gtar", "gtar"),
    X_GZIP("application/x-gzip", "gz"),
    X_HDF("application/x-hdf", "hdf"),
    X_COMPONENT("text/x-component", "htc"),
    X_ICON("image/x-icon", "ico"),
    X_IPHONE("application/x-iphone", "iii"),
    X_INTERNET_SIGNUP("application/x-internet-signup", "ins,isp"),
    X_AIFF("audio/x-aiff", "aif,aifc,aiff"),
    X_MS_ASF("video/x-ms-asf", "asf,asr,asx"),
    X_MSVIDEO("video/x-msvideo", "avi"),
    X_BCPIO("application/x-bcpio", "bcpio"),
    X_CDF("application/x-cdf", "cdf"),
    X_X509_CA_CERT("application/x-x509-ca-cert", "cer,crt,der"),
    X_MSCLIP("application/x-msclip", "clp"),
    X_CMX("image/x-cmx", "cmx"),
    X_CPIO("application/x-cpio", "cpio"),
    X_MSCARDFILE("application/x-mscardfile", "crd"),
    X_CSH("application/x-csh", "csh"),
    X_DIRECTOR("application/x-director", "dcr,dir,dxr"),
    X_MSDOWNLOAD("application/x-msdownload", "dll"),
    X_DVI("application/x-dvi", "dvi"),
    X_SETEXT("text/x-setext", "etx"),
    X_JAVASCRIPT("application/x-javascript", "js"),
    X_LATEX("application/x-latex", "latex"),
    X_LA_ASF("video/x-la-asf", "lsf,lsx"),
    X_MSMEDIAVIEW("application/x-msmediaview", "m13,m14,mvb"),
    X_MPEGURL("audio/x-mpegurl", "m3u"),
    X_TROFF_MAN("application/x-troff-man", "man"),
    X_TROFF_ME("application/x-troff-me", "me"),
    X_TROFF_MS("application/x-troff-ms", "ms"),
    X_MSACCESS("application/x-msaccess", "mdb"),
    X_MSMONEY("application/x-msmoney", "mny"),
    X_SGI_MOVIE("video/x-sgi-movie", "movie"),
    X_PKCS12("application/x-pkcs12", "p12,pfx"),
    X_PKCS7_CERTIFICATES("application/x-pkcs7-certificates", "p7b,spc"),
    X_PKCS7_MIME("application/x-pkcs7-mime", "p7c,p7m"),
    X_PKCS7_CERTREQRESP("application/x-pkcs7-certreqresp", "p7r"),
    X_PKCS7_SIGNATURE("application/x-pkcs7-signature", "p7s"),
    X_PORTABLE_BITMAP("image/x-portable-bitmap", "pbm"),
    X_PORTABLE_GRAYMAP("image/x-portable-graymap", "pgm"),
    X_PORTABLE_ANYMAP("image/x-portable-anymap", "pnm"),
    X_PORTABLE_PIXMAP("image/x-portable-pixmap", "ppm"),
    X_PERFMON("application/x-perfmon", "pma,pmc,pml,pmr,pmw"),
    X_MSPUBLISHER("application/x-mspublisher", "pub"),
    X_PN_REALAUDIO("audio/x-pn-realaudio", "ra,ram"),
    X_CMU_RASTER("image/x-cmu-raster", "ras"),
    X_RGB("image/x-rgb", "rgb"),
    X_MSSCHEDULE("application/x-msschedule", "scd"),
    X_SH("application/x-sh", "sh"),
    X_SHAR("application/x-shar", "shar"),
    X_STUFFIT("application/x-stuffit", "sit"),
    X_TROFF("application/x-troff", "roff,t,tr"),
    X_SV4CPIO("application/x-sv4cpio", "sv4cpio"),
    X_SV4CRC("application/x-sv4crc", "sv4crc"),
    X_SHOCKWAVE_FLASH("application/x-shockwave-flash", "swf"),
    X_TAR("application/x-tar", "tar"),
    X_TCL("application/x-tcl", "tcl"),
    X_TEX("application/x-tex", "tex"),
    X_TEXINFO("application/x-texinfo", "texi,texinfo"),
    X_COMPRESSED("application/x-compressed", "tgz"),
    X_MSTERMINAL("application/x-msterminal", "trm"),
    X_WAIS_SOURCE("application/x-wais-source", "src"),
    X_USTAR("application/x-ustar", "ustar"),
    X_VCARD("text/x-vcard", "vcf"),
    X_WAV("audio/x-wav", "wav"),
    X_MS_WMA("audio/x-ms-wma", "wma"),
    X_MSMETAFILE("application/x-msmetafile", "wmf"),
    X_MS_WMV("video/x-ms-wmv", "wmv"),
    X_MSWRITE("application/x-mswrite", "wri"),
    X_XBITMAP("image/x-xbitmap", "xbm"),
    X_XPIXMAP("image/x-xpixmap", "xpm"),
    X_XWINDOWDUMP("image/x-xwindowdump", "xwd"),
    X_COMPRESS("application/x-compress", "z"),
    PDF("application/pdf", "pdf"),
    PNG("image/png", "png"),
    PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"),
    PPSX("application/vnd.openxmlformats-officedocument.presentationml.slideshow", "ppsx"),
    PICS_RULES("application/pics-rules", "prf"),
    RN_REALMEDIA("application/vnd.rn-realmedia", "rm"),
    MID("audio/mid", "rmi,mid,midi"),
    RTF("application/rtf", "rft"),
    RICHTEXT("text/richtext", "rtx"),
    SCRIPTLET("text/scriptlet", "sct"),
    SET_PAYMENT_INITIATION("application/set-payment-initiation", "setpay"),
    SET_REGISTRATION_INITIATION("application/set-registration-initiation", "setreg"),
    FUTURESPLASH("application/futuresplash", "spl"),
    SVG("image/svg+xml", "svg"),
    TIFF("image/tiff", "tif,tiff"),
    TAB_SEPARATED_VALUES("text/tab-separated-values", "tsv"),
    IULS("text/iuls", "uls"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
    XML("text/xml", "xml"),
    ZIP("application/zip", "zip");

    private static Map<String, MimeType> suffixMapMimeType = new HashMap<String, MimeType>();
    static {
        for (MimeType mime : MimeType.values()) {
            String[] suffixArr = mime.getSuffix().split(",");
            for (String suffix : suffixArr) {
                suffixMapMimeType.put(suffix, mime);
            }
        }
    }

    public static MimeType get(String type) {
        String lowerType = type.toLowerCase();
        for (MimeType mime : MimeType.values()) {
            if (mime.type.equals(lowerType)) {
                return mime;
            }
        }
        return null;
    }

    public static MimeType getBySuffix(String suffix) {
        String lowerSuffix = suffix.toLowerCase();
        return suffixMapMimeType.get(lowerSuffix);
    }

    private String type;

    private String suffix;

    private MimeType(String type, String suffix) {
        this.type = type;
        this.suffix = suffix;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getType() {
        return type;
    }
}
