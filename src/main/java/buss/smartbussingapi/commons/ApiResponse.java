package buss.smartbussingapi.commons;

public record ApiResponse<T> (String info, T response, String error){}
