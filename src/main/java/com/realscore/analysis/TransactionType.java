package com.realscore.analysis;

/** Sale or rent, with Sreality's {@code category_type_cb} code (doc "Sreality API"). */
public enum TransactionType {
    SALE(1),
    RENT(2);

    private final int code;

    TransactionType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    /** @return the type with that code, or {@code null} for {@code null} or an unknown code */
    public static TransactionType ofCode(Integer code) {
        for (TransactionType type : values()) {
            if (code != null && type.code == code) {
                return type;
            }
        }
        return null;
    }
}
