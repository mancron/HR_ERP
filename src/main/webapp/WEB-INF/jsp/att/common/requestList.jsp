<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<table class="att-table">
    <tr>
        <th>기간/날짜</th>
        <th>유형</th>
        <th>상태</th>
    </tr>

    <c:forEach var="item" items="${list}">
        <tr>
            <td>${item.date}</td>

            <td>${item.type}</td>

            <td>
                <button type="button"
                    class="status-btn
                        <c:if test='${item.status eq "승인"}'> status-approved</c:if>
                        <c:if test='${item.status eq "반려"}'> status-rejected</c:if>
                        <c:if test='${item.status eq "대기"}'> status-pending</c:if>"
                    onclick="openDetail('${item.id}')">

                    ${item.status}
                </button>
            </td>
        </tr>
    </c:forEach>
</table>