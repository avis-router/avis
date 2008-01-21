#include <stdexcept>
#include <string>

class ProtocolCodecException : public std::runtime_error
{
public:
	ProtocolCodecException(const char *message) : runtime_error(message) {};
};
