#ifndef INVALID_URI_EXCEPTION_H
#define INVALID_URI_EXCEPTION_H

#include <stdexcept>
#include <string>

class InvalidURIException : public std::runtime_error
{
public:
	InvalidURIException(const char *uri, const char *message);

	const char *GetURIErrorMessage();

protected:
	std::string theerrormsg;
};

#endif //INVALID_URI_EXCEPTION_H
