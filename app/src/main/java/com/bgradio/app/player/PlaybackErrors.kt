package com.bgradio.app.player

import androidx.media3.common.PlaybackException

/** Turns Media3 error codes into short, understandable Bulgarian messages. */
object PlaybackErrors {

    const val NO_INTERNET = "Няма интернет връзка. Провери интернет връзката си."
    const val CONNECT_FAILED = "Неуспешно свързване с радиостанцията."
    const val WIFI_ONLY = "Включена е настройката „Само Wi-Fi“, а телефонът не е свързан към Wi-Fi."

    fun message(error: PlaybackException?, online: Boolean): String {
        if (!online) return NO_INTERNET
        return when (error?.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_TIMEOUT ->
                "Радиостанцията не отговаря. Изтече времето за изчакване."

            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                "$CONNECT_FAILED Провери интернет връзката си."

            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                "Радиостанцията е временно недостъпна."

            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ->
                "Адресът на потока е невалиден или вече не работи."

            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ->
                "Форматът на този поток не се поддържа от телефона."

            else -> CONNECT_FAILED
        }
    }

    /** Network-type problems are worth retrying; broken URLs and codecs are not. */
    fun isRecoverable(error: PlaybackException?): Boolean {
        if (error == null) return true
        val code = error.errorCode
        if (code == PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED) return false
        return code in 2000..2999 ||
            code == PlaybackException.ERROR_CODE_TIMEOUT ||
            code == PlaybackException.ERROR_CODE_UNSPECIFIED ||
            code == PlaybackException.ERROR_CODE_REMOTE_ERROR
    }
}
