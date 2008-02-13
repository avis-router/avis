#include "ElvinURI.h"
#include <sstream>
#include "pcrecpp.h"

ElvinURI::ElvinURI(char *theuriString)
{
	DEFAULT_PROTOCOL.push_back("tcp");
	DEFAULT_PROTOCOL.push_back("none");
	DEFAULT_PROTOCOL.push_back("xdr");
	
	SECURE_PROTOCOL.push_back("ssl");
	SECURE_PROTOCOL.push_back("none");
	SECURE_PROTOCOL.push_back("xdr");
	
	init();
	uriString = theuriString;
	parseUri();
	validate();
}

ElvinURI::ElvinURI(char *host, int port)
: m_port(port)
{
	DEFAULT_PROTOCOL.push_back("tcp");
	DEFAULT_PROTOCOL.push_back("none");
	DEFAULT_PROTOCOL.push_back("xdr");
	
	SECURE_PROTOCOL.push_back("ssl");
	SECURE_PROTOCOL.push_back("none");
	SECURE_PROTOCOL.push_back("xdr");

	init();
	uriString.append("elvin://");
	uriString.append(host);
	uriString.append(":");

	std::ostringstream oss;
	oss << port;
	uriString.append(oss.str());

	scheme = "elvin";
	m_host = host;
}

ElvinURI::ElvinURI(char *theuriString, ElvinURI &defaultUri)
{
	DEFAULT_PROTOCOL.push_back("tcp");
	DEFAULT_PROTOCOL.push_back("none");
	DEFAULT_PROTOCOL.push_back("xdr");
	
	SECURE_PROTOCOL.push_back("ssl");
	SECURE_PROTOCOL.push_back("none");
	SECURE_PROTOCOL.push_back("xdr");

	init(defaultUri);
	uriString = theuriString;
	parseUri();
	validate();
}

ElvinURI::ElvinURI(ElvinURI &defaultUri)
{
	DEFAULT_PROTOCOL.push_back("tcp");
	DEFAULT_PROTOCOL.push_back("none");
	DEFAULT_PROTOCOL.push_back("xdr");
	
	SECURE_PROTOCOL.push_back("ssl");
	SECURE_PROTOCOL.push_back("none");
	SECURE_PROTOCOL.push_back("xdr");

	init(defaultUri);
	validate();
}

ElvinURI::~ElvinURI()
{
}

void ElvinURI::init(ElvinURI &defaultUri)
{
	uriString = defaultUri.uriString;
	scheme = defaultUri.scheme;
	versionMajor = defaultUri.versionMajor;
	versionMinor = defaultUri.versionMinor;
	protocol = defaultUri.protocol;
	m_host = defaultUri.m_host;
	m_port = defaultUri.m_port;
	options = defaultUri.options;
}

void ElvinURI::init()
{
	scheme = "";
	versionMajor = CLIENT_VERSION_MAJOR;
	versionMinor = CLIENT_VERSION_MINOR;
	protocol = DEFAULT_PROTOCOL;
	m_host = "";
	m_port = DEFAULT_PORT;
}

void ElvinURI::validate()
{
	if (!validScheme(scheme.c_str()))
	{
		std::string errormessage;
		errormessage.append("Invalid scheme: ");
		errormessage.append(scheme.c_str());
		throw InvalidURIException(uriString.c_str(), errormessage.c_str());
	}
}

bool ElvinURI::validScheme(const char *schemeToCheck)
{	
	if (strcmp(schemeToCheck, "elvin") == 0)
		return true;
	else
		return false;
}

std::string ElvinURI::toString()
{
	return uriString;
}

/**
* Generate a canonical text version of this URI.
*/
std::string ElvinURI::toCanonicalString()
{
	std::string str;
	str.append(scheme);
	str.append(":");

	std::ostringstream vmajoss;
	vmajoss << versionMajor;
	str.append(vmajoss.str());

	str.append(".");
	
	std::ostringstream vminoss;
	vminoss << versionMinor;
	str.append(vminoss.str());

	str.append("/");

	bool first = true;
	std::list<std::string>::iterator listiter;
	listiter = protocol.begin();
	while (listiter != protocol.end())
	{	
		if (!first)
		{
			str.append(",");
		}
		first = false;
		str.append(*listiter);
		listiter++;
	}
	
	str.append("/");
	str.append(m_host);
	str.append (":");

	std::ostringstream oss;
	oss << m_port;

	str.append(oss.str());

	// NB: options is a sorted map, canonical order is automatic
	std::map<std::string, std::string>::iterator iter;
	iter = options.begin();
	while (iter != options.end())
	{
		str.append(";");
		str.append(iter->first);
		str.append("=");
		str.append(iter->second);
		iter++;
	}

	return str;
}

bool ElvinURI::equals(ElvinURI &uri)
{
	return ((scheme.compare(uri.scheme) == 0 ? true : false) &&
		(m_host.compare(uri.m_host) == 0 ? true : false) &&
		(m_port == uri.m_port) &&
		(versionMajor == uri.versionMajor) &&
		(versionMinor == uri.versionMinor) &&
		(options == uri.options) &&
		(protocol == uri.protocol));
}

void ElvinURI::parseUri()
{ 
	string group1, group2, group3, group4, group5;
	pcrecpp::RE pat("(\\w+):([^/]*)/([^/]*)/([^;/][^;]*)(;.*)?");
	if (pat.FullMatch(uriString, &group1, &group2, &group3, &group4, &group5))
	{
		scheme = group1;

		// version
		if (group2.length() != 0)
		{
			parseVersion(group2.c_str());
		}

		// protocol
		if (group3.length() != 0)
		{
			parseProtocol(group3.c_str());
		}

		// endpoint (host/port)
		parseEndpoint(group4.c_str());

		// options
		if (group5.length() != 0)
		{
			parseOptions(group5.c_str());
		}
	}
	else
	{
		throw InvalidURIException(uriString.c_str(), "Not a valid Elvin URI");
	}
}

void ElvinURI::parseVersion(const char *versionExpr)
{
	string group1, group2, group3;
	pcrecpp::RE pat("(\\d+)(\\.(\\d+))?");
	if (pat.FullMatch(versionExpr, &group1, &group2, &group3))
	{
		std::istringstream versionMajorStream(group1.c_str());
		if (!(versionMajorStream >> versionMajor))
		{
			std::string errormessage;
			errormessage.append("Invalid version string: \"");
			errormessage.append(versionExpr);
			errormessage.append("\"");
			throw InvalidURIException(uriString.c_str(), errormessage.c_str());
		}

		if (group3.length() > 0)
		{
			std::istringstream versionMinorStream(group3.c_str());
			if (!(versionMinorStream >> versionMinor))
			{
				std::string errormessage;
				errormessage.append("Invalid version string: \"");
				errormessage.append(versionExpr);
				errormessage.append("\"");
				throw InvalidURIException(uriString.c_str(), errormessage.c_str());			
			}
		}
	} 
	else
	{
		std::string errormessage;
		errormessage.append("Invalid version string: \"");
		errormessage.append(versionExpr);
		errormessage.append("\"");
		throw InvalidURIException(uriString.c_str(), errormessage.c_str());
	}
}

void ElvinURI::parseProtocol(const char *protocolExpr)
{
	string group1, group2, group3, group4;
	pcrecpp::RE pat("((\\w+),(\\w+),(\\w+))|secure");
	if (pat.FullMatch(protocolExpr, &group1, &group2, &group3, &group4))
	{
		if (group2.length() > 0)
		{
			//clear current protocol list first before adding new entries
			protocol.clear();

			protocol.push_back(group2.c_str());
			protocol.push_back(group3.c_str());
			protocol.push_back(group4.c_str());			
		}
		else
		{	
			protocol = SECURE_PROTOCOL;
		}
	} 
	else
	{
		std::string errormessage;
		errormessage.append("Invalid protocol: \"");
		errormessage.append(protocolExpr);
		errormessage.append("\"");
		throw InvalidURIException(uriString.c_str(), errormessage.c_str());
	}
}

void ElvinURI::parseEndpoint(const char *endpoint)
{
	std::string patternstring;

	// choose between IPv6 and IPv4 address scheme
	if (endpoint[0] == '[')
		patternstring = "(\\[[^\\]]+\\])(:(\\d+))?";
	else
		patternstring =  "([^:]+)(:(\\d+))?";

	string group1, group2, group3;
	pcrecpp::RE pat(patternstring);
	if (pat.FullMatch(endpoint, &group1, &group2, &group3))
	{
		m_host = group1;

		if (group3.length() > 0)
			m_port = atoi (group3.c_str());
	} 
	else
	{
		throw InvalidURIException(uriString.c_str(), "Invalid port number");
	}
}

void ElvinURI::parseOptions(const char *optionsExpr)
{
	pcrecpp::StringPiece input(optionsExpr);

	string group1, group2;
	pcrecpp::RE pat(";([^=;]+)=([^=;]*)");
	int matchcount = 0;
	while (pat.Consume(&input, &group1, &group2)) 
	{
		if (group1.length() > 0)
		{
			if (group1.length() > 0)
			{
				matchcount++;
				options.insert(std::pair<std::string, std::string> (group1, group2));
			}
			else
			{
				std::string errormessage;
				errormessage.append("Invalid options: \"");
				errormessage.append(optionsExpr);
				errormessage.append("\"");
				throw InvalidURIException(uriString.c_str(), errormessage.c_str());
			}
		}
	}
	
	if (matchcount == 0)
	{
		std::string errormessage;
		errormessage.append("Invalid options: \"");
		errormessage.append(optionsExpr);
		errormessage.append("\"");
		throw InvalidURIException(uriString.c_str(), errormessage.c_str());
	}
}

/**
* The default URI protocol stack: "tcp", "none", "xdr"
*/
std::list<std::string> ElvinURI::defaultProtocol()
{
	return DEFAULT_PROTOCOL;
}

/**
* The secure URI protocol stack: "ssl", "none", "xdr"
*/
std::list<std::string> ElvinURI::secureProtocol()
{
	return SECURE_PROTOCOL;
}
