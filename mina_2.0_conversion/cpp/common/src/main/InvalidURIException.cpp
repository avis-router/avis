#include "InvalidURIException.h"

InvalidURIException::InvalidURIException(const char *uri, const char *message)
: std::runtime_error("InvalidURIException")
{
	theerrormsg.append("Invalid URI \"");
	theerrormsg.append(uri);
	theerrormsg.append("\": ");
	theerrormsg.append(message);
}

const char *InvalidURIException::GetURIErrorMessage()
{
	return theerrormsg.c_str();
}
