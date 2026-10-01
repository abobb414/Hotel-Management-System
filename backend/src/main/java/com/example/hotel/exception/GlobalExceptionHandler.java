package com.example.hotel.exception;

import com.example.hotel.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 统一异常出口。
 *
 * <p>业务层沿用英文异常消息，这里在 API 边界统一翻译成中文，保证前端弹窗文案与整体中文界面一致。
 * 这样业务代码无需改动，也不会破坏 {@code SecurityUtil} / {@code JwtTokenProvider} 以
 * {@code "Unauthorized"} 作为控制流标记的既有约定。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 与 {@code SecurityUtil} / {@code JwtTokenProvider} 约定的未登录标记，命中即返回 401。 */
    private static final String UNAUTHORIZED_SENTINEL = "Unauthorized";

    /** 业务异常英文消息 -> 中文提示。 */
    private static final Map<String, String> MESSAGE_TRANSLATIONS = buildMessageTranslations();

    /** 校验字段名 -> 中文字段标签。 */
    private static final Map<String, String> FIELD_LABELS = buildFieldLabels();

    private static final Pattern REQUIRED = Pattern.compile("^(\\w+) is required$");
    private static final Pattern LENGTH_BETWEEN =
            Pattern.compile("^(\\w+) length must be between (\\d+) and (\\d+)$");
    private static final Pattern LENGTH_MAX = Pattern.compile("^(\\w+) length must not exceed (\\d+)$");
    private static final Pattern INVALID_FORMAT = Pattern.compile("^(\\w+) format is invalid$");
    private static final Pattern MIN_VALUE = Pattern.compile("^(\\w+) must be at least (\\d+)$");
    private static final Pattern MAX_VALUE = Pattern.compile("^(\\w+) cannot exceed (\\d+)$");

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        String raw = ex.getMessage();
        if (UNAUTHORIZED_SENTINEL.equals(raw)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("登录状态已失效，请重新登录"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(translateMessage(raw)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> translateValidation(error.getField(), error.getDefaultMessage()))
                .distinct()
                .collect(Collectors.joining("；"));
        return ApiResponse.fail(message.isBlank() ? "提交内容不合法" : message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations()
                .stream()
                .map(violation -> translateMessage(violation.getMessage()))
                .distinct()
                .collect(Collectors.joining("；"));
        return ApiResponse.fail(message.isBlank() ? "提交内容不合法" : message);
    }

    /** 缺少必填查询参数：属于客户端错误，必须返回 400 而不是 500。 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingParameter(MissingServletRequestParameterException ex) {
        String label = FIELD_LABELS.getOrDefault(ex.getParameterName(), ex.getParameterName());
        return ApiResponse.fail("缺少必要参数：" + label);
    }

    /** 参数类型不匹配，例如把非数字传给了 id。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String label = FIELD_LABELS.getOrDefault(ex.getName(), ex.getName());
        return ApiResponse.fail(label + "参数格式不正确");
    }

    /** 请求体不是合法 JSON 或字段类型无法解析。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ApiResponse.fail("请求内容格式不正确，请检查提交的数据");
    }

    /** 接口不存在：应返回 404，而不是被兜底处理器吞成 500。 */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoResource(NoResourceFoundException ex) {
        return ApiResponse.fail("接口不存在：" + ex.getResourcePath());
    }

    /** 请求方法用错，例如对只接受 POST 的接口发了 GET。 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ApiResponse.fail("请求方法不被支持：" + ex.getMethod());
    }

    /**
     * 兜底：记录真实异常用于排查，但对外只返回通用文案，避免把内部异常细节（表名、SQL、堆栈信息）
     * 直接暴露给前端。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("未处理的服务端异常: {}", ex.getMessage(), ex);
        return ApiResponse.fail("服务器处理出错，请稍后重试");
    }

    public static void writeJsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        OBJECT_MAPPER.writeValue(response.getWriter(), ApiResponse.fail(message));
    }

    /** 业务消息翻译，未收录时原样返回，避免丢掉未知提示。 */
    private static String translateMessage(String message) {
        if (message == null || message.isBlank()) {
            return "操作失败";
        }
        return MESSAGE_TRANSLATIONS.getOrDefault(message, message);
    }

    /** 把 Bean Validation 的英文句式消息改写为中文，并套用字段中文标签。 */
    private static String translateValidation(String field, String defaultMessage) {
        String label = FIELD_LABELS.getOrDefault(field, field);
        if (defaultMessage == null || defaultMessage.isBlank()) {
            return label + "不合法";
        }

        Matcher matcher = REQUIRED.matcher(defaultMessage);
        if (matcher.matches()) {
            return "请填写" + label;
        }

        matcher = LENGTH_BETWEEN.matcher(defaultMessage);
        if (matcher.matches()) {
            return label + "长度需为 " + matcher.group(2) + "-" + matcher.group(3) + " 个字符";
        }

        matcher = LENGTH_MAX.matcher(defaultMessage);
        if (matcher.matches()) {
            return label + "长度不能超过 " + matcher.group(2) + " 个字符";
        }

        matcher = INVALID_FORMAT.matcher(defaultMessage);
        if (matcher.matches()) {
            return label + "格式不正确";
        }

        matcher = MIN_VALUE.matcher(defaultMessage);
        if (matcher.matches()) {
            return label + "不能小于 " + matcher.group(2);
        }

        matcher = MAX_VALUE.matcher(defaultMessage);
        if (matcher.matches()) {
            return label + "不能大于 " + matcher.group(2);
        }

        return translateMessage(defaultMessage);
    }

    private static Map<String, String> buildMessageTranslations() {
        Map<String, String> map = new LinkedHashMap<>();
        // 认证与账号
        map.put("Invalid username or password", "用户名或密码不正确");
        map.put("Invalid phone or password", "手机号或密码不正确");
        map.put("Passwords do not match", "两次输入的密码不一致");
        map.put("Old password is incorrect", "原密码不正确");
        map.put("Phone already registered", "该手机号已注册");
        map.put("Username already exists", "用户名已存在");
        map.put("User not found", "用户不存在");
        map.put("Default admin account cannot be deleted", "默认管理员账号不能删除");
        map.put("Unsupported role", "不支持的角色");
        map.put("Unsupported status", "不支持的状态");
        // 房型
        map.put("room type does not exist", "房型不存在");
        map.put("room type is referenced by rooms and cannot be deleted", "该房型已被房间引用，不能删除");
        // 房间
        map.put("room does not exist", "房间不存在");
        map.put("room number already exists", "房间号已存在");
        map.put("occupied room cannot be deleted", "入住中的房间不能删除");
        map.put("room has reservations and cannot be deleted", "该房间存在预订记录，不能删除");
        map.put("room is not available for the selected dates", "该房间在所选日期内已被占用，请另选房间或调整日期");
        map.put("room is not available for the extended dates", "该房间在续住日期内已被占用，无法续住");
        map.put("room under maintenance cannot be reserved", "维修中的房间不能预订");
        map.put("target room does not exist", "目标房间不存在");
        map.put("target room under maintenance cannot be assigned", "维修中的房间不能分配");
        map.put("target room is not available for the reservation dates", "目标房间在该订单日期内已被占用");
        map.put("checkOut must be later than checkIn", "离店日期必须晚于入住日期");
        map.put("checkOutDate must be later than checkInDate", "离店日期必须晚于入住日期");
        map.put("new checkOutDate must be later than current checkOutDate", "新的离店日期必须晚于当前离店日期");
        map.put("stay nights must be greater than 0", "入住晚数必须大于 0");
        // 预订
        map.put("reservation does not exist", "预订订单不存在");
        map.put("completed reservation status cannot be changed", "已完成的订单状态不可变更");
        map.put("completed reservation cannot be modified", "已完成的订单不可修改");
        map.put("unsupported reservation status", "不支持的订单状态");
        map.put("invalid BOOKED transition", "订单状态流转不合法（当前为已预订）");
        map.put("invalid CHECKED_IN transition", "订单状态流转不合法（当前为已入住）");
        // 住客
        map.put("guest does not exist", "住客不存在");
        map.put("guest phone already exists", "该手机号已被其他住客占用");
        map.put("guest idCard already exists", "该身份证号已被其他住客占用");
        map.put("guest phone and idCard belong to different guests", "手机号与身份证号分属不同住客，请核对");
        // 通知
        map.put("notification does not exist", "通知不存在");
        return Map.copyOf(map);
    }

    private static Map<String, String> buildFieldLabels() {
        Map<String, String> map = new LinkedHashMap<>();
        // 查询参数
        map.put("checkIn", "入住日期");
        map.put("checkOut", "离店日期");
        map.put("id", "记录 ID");
        // 账号
        map.put("username", "用户名");
        map.put("password", "密码");
        map.put("confirmPassword", "确认密码");
        map.put("oldPassword", "原密码");
        map.put("newPassword", "新密码");
        map.put("displayName", "显示名称");
        map.put("role", "角色");
        map.put("status", "状态");
        map.put("phone", "手机号");
        // 住客
        map.put("fullName", "姓名");
        map.put("guestName", "入住人姓名");
        map.put("idCard", "身份证号");
        map.put("memberLevel", "会员等级");
        // 房型
        map.put("name", "房型名称");
        map.put("basePrice", "基础房价");
        map.put("maxGuests", "可住人数");
        map.put("bedType", "床型");
        map.put("area", "面积");
        map.put("description", "房型描述");
        map.put("amenities", "配套设施");
        map.put("coverImage", "封面图");
        // 房间
        map.put("roomNumber", "房间号");
        map.put("roomTypeId", "房型");
        map.put("floor", "楼层");
        map.put("cleanStatus", "清洁状态");
        // 预订
        map.put("roomId", "房间");
        map.put("checkInDate", "入住日期");
        map.put("checkOutDate", "离店日期");
        map.put("guestCount", "入住人数");
        map.put("channel", "预订渠道");
        map.put("specialRequest", "特殊需求");
        return Map.copyOf(map);
    }
}
