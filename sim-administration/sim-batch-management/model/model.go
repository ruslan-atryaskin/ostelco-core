package model

// TODO: Delete all the ICCID entries that are not necessary, that would be at
//       about three of them.


// SimEntry represents individual sim profiles.  Instances can be
// subject to JSON serialisation/deserialisation, and can be stored
// in persistent storage.
type SimEntry struct {
	ID                   int64  `db:"id" json:"id"`
	BatchID              int64  `db:"batchID" json:"batchID"`
	RawIccid             string `db:"rawIccid" json:"rawIccid"`
	IccidWithChecksum    string `db:"iccidWithChecksum" json:"iccidWithChecksum"`
	IccidWithoutChecksum string `db:"iccidWithoutChecksum" json:"iccidWithoutChecksum"`
	Iccid                string `db:"iccid" json:"iccid"`
	Imsi                 string `db:"imsi" json:"imsi"`
	Msisdn               string `db:"msisdn" json:"msisdn"`
	Ki                   string `db:"ki" json:"ki"`
	ActivationCode       string `db:"activationCode" json:"activationCode"`
}

// Batch represents batches of sim profiles.  Instances can be
// subject to JSON serialisation/deserialisation, and can be stored
// in persistent storage.
type Batch struct {
	BatchID int64  `db:"id" json:"id"` // TODO: SHould this be called 'Id'
	Name    string `db:"name" json:"name"`

	// TODO: Customer is a misnomer: This is the customer name used when
	//       ordering a sim batch, used in the input file.  So a very
	//       specific use, not in any way the generic thing the word
	//       as it is used now points to.

	FilenameBase    string `db:"filenameBase" json:"filenameBase"`
	Customer        string `db:"customer" json:"customer"`
	ProfileType     string `db:"profileType" json:"profileType"`
	OrderDate       string `db:"orderDate" json:"orderDate"`
	BatchNo         string `db:"batchNo" json:"batchNo"`
	Quantity        int    `db:"quantity" json:"quantity"`
	FirstIccid      string `db:"firstIccid" json:"firstIccid"`
	FirstImsi       string `db:"firstImsi" json:"firstImsi"`
	URL             string `db:"url" json:"url"`
	MsisdnIncrement int    `db:"msisdnIncrement" json:"msisdnIncrement"`
	IccidIncrement  int    `db:"iccidIncrement" json:"iccidIncrement"`
	ImsiIncrement   int    `db:"imsiIncrement" json:"imsiIncrement"`
	FirstMsisdn     string `db:"firstMsisdn" json:"firstMsisdn"`
	ProfileVendor   string `db:"profileVendor" json:"profileVendor"`
}


// ProfileVendor represents sim profile vendors.  Instances can be
// subject to JSON serialisation/deserialisation, and can be stored
// in persistent storage.
type ProfileVendor struct {
	ID                 int64  `db:"id" json:"id"`
	Name               string `db:"name" json:"name"`
	Es2PlusCert        string `db:"es2PlusCertPath" json:"es2plusCertPath"`
	Es2PlusKey         string `db:"es2PlusKeyPath"  json:"es2PlusKeyPath"`
	Es2PlusHost        string `db:"es2PlusHostPath" json:"es2plusHostPath"`
	Es2PlusPort        int    `db:"es2PlusPort" json:"es2plusPort"`
	Es2PlusRequesterID string `db:"es2PlusRequesterId" json:"es2PlusRequesterId"`
}
