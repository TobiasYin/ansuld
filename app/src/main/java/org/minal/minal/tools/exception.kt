package org.minal.minal.exception

enum class ErrorCode(val code: Int, val msg: String) {
    VNC_CONN_TO_SERVER_BREAK(1001, "Lose connection with server..."),
    VNC_CONN_TO_CLIENT_BREAK(1002, "Lose connection with client..."),
    VNC_CLIENT_INIT_FAILED(1003, "Init client failed"),
    VNC_DISPLAY_CHANGED(1004, "Display down..."),
    VNC_DISPLAY_NOT_FOUND(1004, "Display not found"),
    PRESENTATION_DISMISS(1005, "Second display dismiss");

    override fun toString(): String {
        return "ErrorCode(code=$code, msg=$msg)"
    }
}

open class BaseException(open val errorCode: ErrorCode, cause: Throwable?) :
    Exception(errorCode.msg, cause)

open class VncException(override val errorCode: ErrorCode, cause: Throwable?) :
    BaseException(errorCode, cause)

class ClientInitFailedException(cause: Throwable?) :
    VncException(ErrorCode.VNC_CLIENT_INIT_FAILED, cause)

class ClientConnectionBreakException(cause: Throwable?) :
    VncException(ErrorCode.VNC_CONN_TO_CLIENT_BREAK, cause)


