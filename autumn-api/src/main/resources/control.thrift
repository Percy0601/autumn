namespace java autumn.api
struct User {
    1: required i32 userId;
    2: required string username;
    3: optional map<string, string> desc;
}

service ControlApi {
    string health();
	string info(1: string msg);
}