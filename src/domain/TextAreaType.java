package domain;

public enum TextAreaType {
	//用于标识多个文本框的类型，便于从数据库中查找和自动保存
	SubDomain,
	RelatedDomain,
	SimilarDomain,
	Email,
	SimilarEmail,
	IPSetOfSubnet,
	IPSetOfCert,
	SpecialPortTarget,
	PackageName,
	BlackIP,
}
