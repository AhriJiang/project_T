package com.juntao.commons.po;

import java.io.Serializable;

public interface IPo<S extends Serializable> extends Serializable {
	String FLAG_YES = "1";
	String FLAG_NO = "0";

	S getId();

	void setId(S id);
}
