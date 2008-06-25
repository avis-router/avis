#ifndef CONNECTION_OPTIONS_H
#define CONNECTION_OPTIONS_H

#include <map>
#include <string>

class ConnectionOptions
{
public:
	ConnectionOptions() {};
	~ConnectionOptions() {};

	int size() { return stringoptions.size() + intoptions.size(); };

private:
	std::map<std::string, std::string> stringoptions;
	std::map<std::string, int> intoptions;

};

#endif CONNECTION_OPTIONS_H
