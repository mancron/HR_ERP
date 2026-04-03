<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:if test="${not empty msg}">
    <script>
        window.addEventListener("DOMContentLoaded", function() {
            showToast("${msg}", "success");
        });
    </script>
</c:if>

<c:if test="${not empty errorMsg}">
    <script>
        window.addEventListener("DOMContentLoaded", function() {
            showToast("${errorMsg}", "error");
        });
    </script>
</c:if>