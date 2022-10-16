namespace java autumn.api

service ControlApi {
    string health();
	string applicationInfo();
	string providerInfo();
	string referenceConfig(1: required string name);
}