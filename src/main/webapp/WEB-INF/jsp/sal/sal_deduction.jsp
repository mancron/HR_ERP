<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>ERP-HRMS - 공제율 관리</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/sal/deduction.css">
</head>
<body>
    <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />
    <div id="main-wrapper">
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <main class="app-content">
            <h1 class="page-title">공제율 관리</h1>

            <%-- 성공/에러 메시지 --%>
            <c:if test="${not empty successMsg}">
                <div class="alert alert-success">✅ <c:out value="${successMsg}" /></div>
            </c:if>
            <c:if test="${not empty errorMsg}">
                <div class="alert alert-error">⚠ <c:out value="${errorMsg}" /></div>
            </c:if>

            <%-- 장기요양율 안내 --%>
            <div class="warn-box">
                ⚠ 장기요양율은 건강보험료에 곱하는 요율입니다. (예: 건강보험료 × 장기요양율)
            </div>

            <div class="card" style="max-width:750px;">
                <div class="card-header">
                    <span class="card-title">📊 연도별 공제율</span>
                    <button type="button" class="btn btn-primary btn-sm"
                            onclick="toggleAddForm()">+ 신규 연도 추가</button>
                </div>

                <%-- 추가 폼 --%>
                <div id="addFormWrap" style="display:none;">
                    <form action="${pageContext.request.contextPath}/sal/deduction"
                          method="post" class="rate-form">
                        <input type="hidden" name="action" value="add">
                        <div class="form-grid">
                            <div class="form-group">
                                <label class="form-label">연도 <span class="required">*</span></label>
                                <input type="number" name="targetYear" class="form-control"
                                       placeholder="예: 2026" min="2000" max="2100" required>
                            </div>
                            <div class="form-group">
                                <label class="form-label">국민연금율 <span class="required">*</span></label>
                                <div class="input-wrap">
                                    <input type="number" name="nationalPensionRate" class="form-control"
                                           placeholder="4.500" step="0.001" min="0" max="10" required>
                                    <span class="input-unit">%</span>
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="form-label">건강보험율 <span class="required">*</span></label>
                                <div class="input-wrap">
                                    <input type="number" name="healthInsuranceRate" class="form-control"
                                           placeholder="3.545" step="0.001" min="0" max="10" required>
                                    <span class="input-unit">%</span>
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="form-label">장기요양율 <span class="required">*</span></label>
                                <div class="input-wrap">
                                    <input type="number" name="longTermCareRate" class="form-control"
                                           placeholder="12.950" step="0.001" min="0" max="30" required>
                                    <span class="input-unit">%</span>
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="form-label">고용보험율 <span class="required">*</span></label>
                                <div class="input-wrap">
                                    <input type="number" name="employmentInsuranceRate" class="form-control"
                                           placeholder="0.900" step="0.001" min="0" max="10" required>
                                    <span class="input-unit">%</span>
                                </div>
                            </div>
                        </div>
                        <div class="btn-row">
                            <button type="button" class="btn btn-secondary"
                                    onclick="toggleAddForm()">취소</button>
                            <button type="submit" class="btn btn-primary">추가</button>
                        </div>
                    </form>
                </div>

                <%-- 수정 폼 (edit 파라미터가 있을 때만 표시) --%>
                <c:if test="${not empty editTarget}">
                    <div class="edit-form-wrap">
                        <div class="edit-form-title">
                            ✏ <c:out value="${editTarget.targetYear}" />년 공제율 수정
                        </div>
                        <form action="${pageContext.request.contextPath}/sal/deduction"
                              method="post" class="rate-form">
                            <input type="hidden" name="action" value="update">
                            <input type="hidden" name="rateId" value="${editTarget.rateId}">
                            <div class="form-grid">
                                <div class="form-group">
                                    <label class="form-label">국민연금율</label>
                                    <div class="input-wrap">
                                        <input type="number" name="nationalPensionRate" class="form-control"
                                               value="${editTarget.nationalPensionRate}"
                                               step="0.00001" min="0" max="0.1" required>
                                        <span class="input-unit">%</span>
                                    </div>
                                </div>
                                <div class="form-group">
                                    <label class="form-label">건강보험율</label>
                                    <div class="input-wrap">
                                        <input type="number" name="healthInsuranceRate" class="form-control"
                                               value="${editTarget.healthInsuranceRate}"
                                               step="0.00001" min="0" max="0.1" required>
                                        <span class="input-unit">%</span>
                                    </div>
                                </div>
                                <div class="form-group">
                                    <label class="form-label">장기요양율</label>
                                    <div class="input-wrap">
                                        <input type="number" name="longTermCareRate" class="form-control"
                                               value="${editTarget.longTermCareRate}"
                                               step="0.00001" min="0" max="0.5" required>
                                        <span class="input-unit">%</span>
                                    </div>
                                </div>
                                <div class="form-group">
                                    <label class="form-label">고용보험율</label>
                                    <div class="input-wrap">
                                        <input type="number" name="employmentInsuranceRate" class="form-control"
                                               value="${editTarget.employmentInsuranceRate}"
                                               step="0.00001" min="0" max="0.05" required>
                                        <span class="input-unit">%</span>
                                    </div>
                                </div>
                            </div>
                            <div class="btn-row">
                                <a href="${pageContext.request.contextPath}/sal/deduction"
                                   class="btn btn-secondary">취소</a>
                                <button type="submit" class="btn btn-primary">수정 완료</button>
                            </div>
                        </form>
                    </div>
                </c:if>

                <%-- 목록 테이블 --%>
                <div class="table-wrap">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>연도</th>
                                <th>국민연금</th>
                                <th>건강보험</th>
                                <th>장기요양</th>
                                <th>고용보험</th>
                                <th>수정</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${empty rateList}">
                                    <tr>
                                        <td colspan="6" class="empty-row">등록된 공제율이 없습니다.</td>
                                    </tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach var="rate" items="${rateList}">
                                        <tr class="${rate.currentYear ? 'row-current' : ''}">
                                            <td>
                                                <strong><c:out value="${rate.targetYear}" />년</strong>
                                                <c:if test="${rate.currentYear}">
                                                    <span class="badge badge-blue">현재</span>
                                                </c:if>
                                            </td>
                                            <td>
                                                <fmt:formatNumber value="${rate.nationalPensionRate * 100}"
                                                                  pattern="0.000" />%
                                            </td>
                                            <td>
                                                <fmt:formatNumber value="${rate.healthInsuranceRate * 100}"
                                                                  pattern="0.000" />%
                                            </td>
                                            <td>
                                                <fmt:formatNumber value="${rate.longTermCareRate * 100}"
                                                                  pattern="0.000" />%
                                            </td>
                                            <td>
                                                <fmt:formatNumber value="${rate.employmentInsuranceRate * 100}"
                                                                  pattern="0.000" />%
                                            </td>
                                            <td>
                                                <a href="${pageContext.request.contextPath}/sal/deduction?edit=${rate.rateId}"
                                                   class="btn btn-secondary btn-sm">수정</a>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>
            </div>

        </main>
    </div>

    <script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
    <script>
        function toggleAddForm() {
            var wrap = document.getElementById('addFormWrap');
            wrap.style.display = (wrap.style.display === 'none') ? 'block' : 'none';
        }
    </script>
</body>
</html>