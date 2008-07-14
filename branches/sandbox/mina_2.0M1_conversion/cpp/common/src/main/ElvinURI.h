#ifndef ELVIN_URI_H
#define ELVIN_URI_H

/**
 * An Elvin URI identifying an Elvin router as described in the "Elvin
 * URI Scheme" specification at
 * http://elvin.org/specs/draft-elvin-uri-prelim-02.txt. The most
 * common Elvin URI for a TCP endpoint is of the form (sections in in []
 * are optional):
 * 
 * <pre>
 *   elvin:[version]/[protocol]/hostname[:port][;options]
 *   
 *   version:  protocol version major.minor form e.g. "4.0"
 *   protocol: protocol stack in transport,security,marshalling order
 *             e.g. "tcp,none,xdr". Alternatively the alias "secure"
 *             can be used to denote the default secure stack
 *             ("ssl,none,xdr").
 *   options:  name1=value1[;name2=value2]* e.g. foo=bar;black=white
 * </pre>
 * 
 * <p>
 * 
 * Example URI 1: <code>elvin://localhost:2917</code>
 * <p>
 * Example URI 2: <code>elvin://192.168.0.2:2917;foo=true</code>
 * <p>
 * Example URI 3: <code>elvin:4.0/ssl,none,xdr/localhost:443</code>
 * 
 */
#include <string>
#include <list>
#include <map>
#include "InvalidURIException.h"

#define CLIENT_VERSION_MAJOR	4
#define CLIENT_VERSION_MINOR	0
#define DEFAULT_PORT			2917

class ElvinURI
{
public:
	ElvinURI(char *theuriString) throw(InvalidURIException);
	ElvinURI(char *host, int port);
	ElvinURI(char *theuriString, ElvinURI &defaultUri) throw(InvalidURIException);
	ElvinURI(ElvinURI &defaultUri);
	~ElvinURI();

	std::string toString();
	std::string toCanonicalString();
	bool equals(ElvinURI &uri);
	std::list<std::string> defaultProtocol();
	std::list<std::string> secureProtocol();

	//accessor methods
	int GetVersionMajor() { return versionMajor; };
	int GetVersionMinor() { return versionMinor; };
	std::string GetHost() { return m_host; };
	int GetPort() { return m_port; };
	std::string GetScheme() { return scheme; };
	std::list<std::string> GetProtocol() { return protocol; };
	std::map<std::string, std::string> GetOptions() { return options; };

protected:

	void init(ElvinURI &defaultUri);
	void init();
	bool validScheme(const char *schemeToCheck);

private:
	std::list<std::string> DEFAULT_PROTOCOL;
	std::list<std::string> SECURE_PROTOCOL;
	std::string uriString;
	std::string scheme;
	int versionMajor;
	int versionMinor;
	std::list<std::string> protocol;
	std::string m_host;
	int m_port;
	std::map<std::string, std::string> options;

	void validate();
	void parseUri() throw(InvalidURIException);
	void parseVersion(const char *versionExpr) throw(InvalidURIException);
	void parseProtocol(const char *protocolExpr) throw(InvalidURIException);
	void parseEndpoint(const char *endpoint) throw(InvalidURIException);
	void parseOptions(const char *optionsExpr) throw(InvalidURIException);
};

#endif //ELVIN_URI_H