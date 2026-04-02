package com.hrms.sys.service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.sys.dao.HolidayDAO;
import com.hrms.sys.dto.HolidayDTO;

import io.github.cdimascio.dotenv.Dotenv;

public class HolidayService {

    private final HolidayDAO holidayDAO = new HolidayDAO();

    private static final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing() 
            .load();

    private static final String API_SERVICE_KEY =
            (dotenv != null) ? dotenv.get("HOLIDAY_API_KEY", "") : "";
    
    private static final String API_BASE_URL =
        "http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo";

    // 요일 한글 변환 배열
    private static final String[] DAY_OF_WEEK_KO = {"", "월", "화", "수", "목", "금", "토", "일"};

    // ──────────────────────────────────────
    // 조회
    // ──────────────────────────────────────

    public List<HolidayDTO> getHolidaysByYear(int year) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            List<HolidayDTO> list = holidayDAO.selectByYear(year, conn);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (HolidayDTO dto : list) {
                // 요일 계산
                DayOfWeek dow = dto.getHolidayDate().getDayOfWeek();
                dto.setDayOfWeek(DAY_OF_WEEK_KO[dow.getValue()]);

                // 날짜 문자열 포맷 (JSP 출력용)
                dto.setHolidayDateStr(dto.getHolidayDate().format(fmt));
            }
            return list;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("공휴일 조회 중 오류가 발생했습니다.", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    // ──────────────────────────────────────
    // 수동 추가
    // ──────────────────────────────────────

    public void addHoliday(String holidayDateStr, String holidayName) {
        LocalDate holidayDate = LocalDate.parse(holidayDateStr);
        int year = holidayDate.getYear();

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();

            // 사전 중복 검증 (UNIQUE KEY 위반 방지)
            if (holidayDAO.existsByDateAndName(holidayDate, holidayName, conn)) {
                throw new RuntimeException("이미 등록된 공휴일입니다. (" + holidayDateStr + " / " + holidayName + ")");
            }

            HolidayDTO dto = new HolidayDTO();
            dto.setHolidayDate(holidayDate);
            dto.setHolidayName(holidayName.trim());
            dto.setHolidayYear(year);

            holidayDAO.insert(dto, conn);

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("공휴일 추가 중 오류가 발생했습니다.", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    // ──────────────────────────────────────
    // 삭제
    // ──────────────────────────────────────

    public void deleteHoliday(int holidayId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            int deleted = holidayDAO.deleteById(holidayId, conn);
            if (deleted == 0) {
                throw new RuntimeException("삭제할 공휴일을 찾을 수 없습니다. (id=" + holidayId + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("공휴일 삭제 중 오류가 발생했습니다.", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    // ──────────────────────────────────────
    // 공공데이터포털 API 자동 적재
    // ──────────────────────────────────────

    /**
     * @param year 적재할 연도
     * @return 새로 추가된 건수 (중복 제외)
     */
    public int fetchAndSaveFromApi(int year) {
        List<HolidayDTO> apiList = callHolidayApi(year);
        if (apiList.isEmpty()) return 0;

        int savedCount = 0;
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // ── 트랜잭션 시작

            for (HolidayDTO dto : apiList) {
                // 중복이면 건너뜀 (같은 날짜+이름이 이미 있으면 스킵)
                if (holidayDAO.existsByDateAndName(dto.getHolidayDate(), dto.getHolidayName(), conn)) {
                    continue;
                }
                holidayDAO.insert(dto, conn);
                savedCount++;
            }

            conn.commit(); // ── 커밋
            return savedCount;

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException re) { re.printStackTrace(); }
            e.printStackTrace();
            throw new RuntimeException("API 데이터 저장 중 오류가 발생했습니다.", e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    // ──────────────────────────────────────
    // Private: API 호출 및 XML 파싱
    // ──────────────────────────────────────

    private List<HolidayDTO> callHolidayApi(int year) {
        List<HolidayDTO> list = new ArrayList<>();
        // 월별로 1~12월 순회 (API가 월 단위로 조회)
        for (int month = 1; month <= 12; month++) {
            try {
                String urlStr = API_BASE_URL
                    + "?serviceKey=" + API_SERVICE_KEY
                    + "&solYear=" + year
                    + "&solMonth=" + String.format("%02d", month)
                    + "&numOfRows=30"
                    + "&_type=xml";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() != 200) continue;

                InputStream is = conn.getInputStream();
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                Document doc = factory.newDocumentBuilder().parse(is);
                doc.getDocumentElement().normalize();

                NodeList items = doc.getElementsByTagName("item");
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");

                for (int i = 0; i < items.getLength(); i++) {
                    Element item = (Element) items.item(i);

                    String dateName = getTagValue("dateName", item);
                    String locdate  = getTagValue("locdate",  item);
                    String isHoliday = getTagValue("isHoliday", item);

                    // isHoliday = "Y" 인 것만 저장 (대체휴일 포함)
                    if (!"Y".equals(isHoliday) || dateName == null || locdate == null) continue;

                    LocalDate date = LocalDate.parse(locdate, fmt);
                    HolidayDTO dto = new HolidayDTO();
                    dto.setHolidayDate(date);
                    dto.setHolidayName(dateName.trim());
                    dto.setHolidayYear(year);
                    list.add(dto);
                }
                is.close();

            } catch (Exception e) {
                // 특정 월 API 실패 시 해당 월만 건너뛰고 계속 진행
                e.printStackTrace();
            }
        }
        return list;
    }

    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() == 0) return null;
        Node node = nodeList.item(0).getFirstChild();
        return (node != null) ? node.getNodeValue() : null;
    }
    public static boolean isApiKeyMissing() {
        // 현재 작업 디렉토리 확인용 (확인 후 삭제)
        System.out.println("▶ 현재 작업 디렉토리: " + System.getProperty("user.dir"));
        System.out.println("▶ API_SERVICE_KEY: " + API_SERVICE_KEY);
        return API_SERVICE_KEY == null || API_SERVICE_KEY.trim().isEmpty();
    }
    
    
}