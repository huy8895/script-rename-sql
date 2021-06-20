package app;

import dao.Reader;
import dao.Writer;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App {
    private final static String SPACE = " ";
    private final static String SINGLE_QUOTE = "'";
    private final static String DISTRICT_SQL = "src/main/java/location_data/district.sql";
    private final static String PROVINCE_SQL = "src/main/java/location_data/province.sql";
    private final static String VIEW_PROVINCE_DISTRICT_WARD_SQL = "src/main/java/location_data/view_province_district_ward.sql";
    private final static String VIEW_PROVINCE_DISTRICT_WARD_CHANGED_AT_SQL = "src/main/java/location_data/view_province_district_ward_changed_at.sql";
    private final static String WARD_SQL = "src/main/java/location_data/ward.sql";
    private final static String FILE_DANH_SACH_TINH = "src/main/java/location_data/sanhsachtinh.txt";
    private final static String NEW_DSTINH = "src/main/java/output/newDStinh.txt";
    private final static String NEW_FILE_DISTRICT_SQL = "src/main/java/output/newDistrict.sql";
    private final static String NEW_FILE_PROVINCE_SQL = "src/main/java/output/newProvince.sql";
    private final static String NEW_FILE_VIEW_PROVINCE_DISTRICT_WARD_SQL = "src/main/java/output/newView_province_district_ward_changed_at.sql";
    private final static String NEW_FILE_VIEW_PROVINCE_DISTRICT_WARD_CHANGED_AT_SQL = "src/main/java/output/newView_province_district_ward.sql";
    private final static String NEW_FILE_WARD_SQL = "src/main/java/output/newWard.sql";

    static final String REGEX_PROVINCE_ID_SQL = "(\\s'[0-9]{2}')";
    static final String REGEX_DISTRICT_ID_SQL = "(\\s'[0-9]{3}')";
    static final String REGEX_WARD_SQL = "(\\s'[0-9]{5}')";

    private final static String FILE_DUPLICATE_WARD_SQL = "src/main/java/output/duplicateWardId.sql";
    private final static List<String> DUPLICATE_WARDS = new ArrayList<>();

    //ma ward = ma dictrict da tao + cac chu cai dau trong ten cua xa/phuong + so thu tu vd: 001


    public static void main(String[] args) throws IOException {


        // tao 1 fileDanhSachTinh moi tu sql va file excel.
        createNewDSTinh(PROVINCE_SQL, FILE_DANH_SACH_TINH, NEW_DSTINH);

        //map_province: id,(province)
        Map<Long, Province> provinceMap = readProvince(NEW_DSTINH);

        //map_district: id,(district)
        Map<Long, District> districtMap = getMapDistrict(DISTRICT_SQL, provinceMap);

        //map_ward: id,(ward)
        Map<Long, Ward> wardMap = getMapWard(WARD_SQL, districtMap);

        //write province to file:
        final List<String> newProvinceSql = getNewProvinceSql(provinceMap);
        final List<String> newDistrictSql = getNewDistrictSql(districtMap, provinceMap);
        final List<String> newWardSql = getNewWardSql(wardMap, districtMap);


        listCodeDuplicateAndVnTelex(provinceMap);
        listCodeDuplicateAndVnTelex(districtMap);
        listCodeDuplicateAndVnTelex(wardMap);

        Writer.write(newProvinceSql, NEW_FILE_PROVINCE_SQL);
        Writer.write(newDistrictSql, NEW_FILE_DISTRICT_SQL);
        Writer.write(newWardSql, NEW_FILE_WARD_SQL);
        Writer.write(DUPLICATE_WARDS, FILE_DUPLICATE_WARD_SQL);

    }

    private static void listCodeDuplicateAndVnTelex(Map<Long, ? extends Model> newProvinceSql) {
        System.out.println("-------- start check duplicate ----------");
        final long countDuplicate = newProvinceSql.values()
                                                  .stream()
                                                  .map(Model::getCode)
                                                  .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                                                  .entrySet()
                                                  .stream()
                                                  .filter(m -> m.getValue() > 1)
                                                  .peek(System.out::println)
                                                  .count();
        System.out.printf("========== count duplicate: %s ========== \n", countDuplicate);
        System.out.println("-------- start check TELEX ----------");
        final long countTelex = newProvinceSql.values()
                                              .stream()
                                              .map(Model::getCode)
                                              .filter(s -> !s.matches("[\\w=]+"))
                                              .peek(System.out::println)
                                              .count();
        System.out.printf("========== count Telex: %s ==========\n", countTelex);
    }

    private static List<String> getNewWardSql(Map<Long, Ward> wardMap, Map<Long, District> districtMap) throws IOException {
        List<String> districtSql = Reader.read(WARD_SQL);
        final List<String> newProvinceSQLS = districtSql.stream()
                                                        .map(line -> replaceDistrictIdAndWardId(line, wardMap, districtMap))
                                                        .collect(Collectors.toList());

        return newProvinceSQLS;
    }

    private static String replaceDistrictIdAndWardId(String line, Map<Long, Ward> wardMap, Map<Long, District> districtMap) {
        final List<String> items = getItems(line);
        final String districtId = items.get(1);
        final String wardId = items.get(3);
        final Ward ward = wardMap.get(Long.valueOf(wardId));
        final District district = districtMap.get(Long.valueOf(districtId));
        final String regexDistrict = REGEX_DISTRICT_ID_SQL.replaceAll("[(|)]", "");
        final String regexWard = REGEX_WARD_SQL.replaceAll("[(|)]", "");

        final String s = line.replaceAll(regexDistrict, getString(district.getCode()))
                             .replaceAll(regexWard, getString(ward.getCode()));
        return s;
    }

    private static List<String> getNewDistrictSql(Map<Long, District> districtMap, Map<Long, Province> provinceMap) throws IOException {
        List<String> districtSql = Reader.read(DISTRICT_SQL);
        final List<String> newProvinceSQLS = districtSql.stream()
                                                        .map(line -> replaceDistrictIdAndProvinceId(line, districtMap, provinceMap))
                                                        .collect(Collectors.toList());
        return newProvinceSQLS;
    }

    private static String replaceDistrictIdAndProvinceId(String line, Map<Long, District> districtMap, Map<Long, Province> provinceMap) {
        final List<String> items = getItems(line);
        final String districtId = items.get(1);
        final String provinceId = items.get(3);
        final Province province = provinceMap.get(Long.valueOf(provinceId));
        final District district = districtMap.get(Long.valueOf(districtId));
        final String regexProvince = REGEX_PROVINCE_ID_SQL.replaceAll("[(|)]", "");
        final String regexDistrict = REGEX_DISTRICT_ID_SQL.replaceAll("[(|)]", "");
        if (province == null || district == null) {
            System.out.println("province = " + province);
            System.out.println("district = " + district);
        }
        return line.replaceAll(regexProvince, getString(province.getCode()))
                   .replaceAll(regexDistrict, getString(district.getCode()));
    }

    private static List<String> getNewProvinceSql(Map<Long, Province> provinceMap) throws IOException {
        List<String> provinceSQLs = Reader.read(PROVINCE_SQL);
        final List<String> newProvinceSQLS = provinceSQLs.stream()
                                                         .map(line -> replaceProvinceId(line, REGEX_PROVINCE_ID_SQL, provinceMap))
                                                         .collect(Collectors.toList());
        return newProvinceSQLS;
    }

    private static Map<Long, Ward> getMapWard(String wardSql, Map<Long, District> districtsMap) throws IOException {
        Map<Long, Ward> wardMap = new HashMap<>();
        Reader.read(wardSql)
              .forEach(wardSqlLine -> putWardMap(wardSqlLine, wardMap, districtsMap));
        return wardMap;
    }

    private static void putWardMap(String wardSqlLine, Map<Long, Ward> wardMap, Map<Long, District> districtsMap) {
        final Ward ward = getWard(wardSqlLine, districtsMap);

        if (ward != null && ward.getId()
                                .equals("26074")) {
            System.out.println("ward = " + ward);
        }
        if (ward == null || wardMap.containsKey(Long.valueOf(ward.getId()))) {
            duplicateWard(wardSqlLine);
        }
        if (ward != null) {
            wardMap.put(Long.valueOf(ward.getId()), ward);
        }
    }

    private static void duplicateWard(String wardSqlLine) {
        DUPLICATE_WARDS.add(wardSqlLine);
    }

    //(id, district_id, is_active, type1, ward_id, ward_name, type) VALUES
    //      ('1863', '132', 1, '', '04552', 'Phúc Lộc', null);
    private static Ward getWard(String wardSqlLine, Map<Long, District> districtsMap) {
        final List<String> items = getItems(wardSqlLine);
        final District district = districtsMap.get(Long.valueOf(items.get(1)));
        final String wardName = items.get(4);
        final Ward ward = Ward.builder()
                              .id(items.get(3))
                              .name(wardName)
                              .districtId(items.get(1))
                              .build();
        if (district != null) {
            ward.setDistrictCode(district.getCode());
            final String wardCode = generateWardCode(district.getCode(), wardName);
            ward.setCode(wardCode);
        }
        return ward;
    }

    private static String generateWardCode(String districtCode, String wardName) {
        final String wardCode = districtCode + getFirstUpWord(wardName);
        return wardCode;
    }

    private static Map<Long, District> getMapDistrict(String districtSql, Map<Long, Province> provinceMap) throws IOException {
        Map<Long, District> districtsMap = new HashMap<>();
        Reader.read(districtSql)
              .forEach(district -> putDistrictMap(district, districtsMap, provinceMap));
        return districtsMap;
    }

    //TODO: put District to map from single district sql;
    private static void putDistrictMap(String districtSqlLine, Map<Long, District> districtsMap, Map<Long, Province> provinceMap) {
        final District d = getDistrict(districtSqlLine, provinceMap, districtsMap);
        if (d != null) {
            districtsMap.put(Long.valueOf(d.getId()), d);
        }
    }

    //TODO: get District from district sql line;
    //(id, district_id, district_name, is_active, province_id, type) VALUES ('763', '994', 'Thị xã Hoài Nhơn', 1, '52', null);
    private static District getDistrict(String districtSqlLine, Map<Long, Province> provinceMap, Map<Long, District> districtsMap) {
        List<String> items = getItems(districtSqlLine);
        final Province province = provinceMap.get(Long.parseLong(items.get(3)));
        final String districtName = items.get(2);
        District district = District.builder()
                                    .id(items.get(1))
                                    .name(districtName)
                                    .provinceId(items.get(3))
                                    .build();
        if (province != null) {
            district.setProvinceCode(province.getCode());
            final String districtCode = generateDistrictCode(province.getCode(), districtName, districtsMap);
            district.setCode(districtCode);
        }

        return district;
    }

    private static void addToDistrictMap(Map<Long, District> districtMap, String districtCode, int index) {
        String indexString = "";
        if (index < 10) {
            indexString = "00" + index;
        } else if (index < 100) {
            indexString = "0" + index;
        }
        String newDistrictCode = districtCode + indexString;
        if (districtMap.containsKey(newDistrictCode)) {
            index++;
            addToDistrictMap(districtMap, districtCode, index);
        } else {
        }
    }


    //HNTH001 ma dictrict = ma province + cac chu cai dau cua ten huyen/quan + so thu tu vd: 001 (tang dan neu ma ton tai ma truoc do)
    private static String generateDistrictCode(String provinceCode, String districtName, Map<Long, District> districtsMap) {
        final String districtCode = provinceCode + getFirstUpWord(districtName);
        return districtCode;
    }

    private static String getFirstUpWord(String name) {
        final String[] split = name.trim()
                                   .replaceAll("\"", "")
                                   .split("\\s+|-");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            try {
                final String word = split[i];
                if (word.matches("\\d+")) {
                    result.append(word);

                } else {

                    final char charAt = word.toUpperCase()
                                            .charAt(0);
                    result.append(charAt);
                }

            } catch (StringIndexOutOfBoundsException e) {
                System.out.println("name = " + name);
            }
        }
        return result.toString();
    }

    private static String replaceProvinceId(String line, String regexProvinceSQL, Map<Long, Province> map) {
        final String provinceId = getId(line, regexProvinceSQL);
        final Province province = map.get(Long.valueOf(provinceId));
        final String target = regexProvinceSQL.replaceAll("[(|)]", "");
        return line.replaceAll(target, getString(province.getCode()));
    }

    private static String getString(String code) {
        return SPACE + SINGLE_QUOTE + code + SINGLE_QUOTE;
    }

    private static String getId(String line, String regex) {
        Matcher matcher = Pattern.compile(regex)
                                 .matcher(line);
        if (matcher.find()) {
            return matcher.group(1)
                          .trim()
                          .replaceAll("'", "");
        }
        return "";
    }

    private static Map<Long, Province> readProvince(String newDStinh) throws IOException {
        Map<Long, Province> map = new HashMap<>();
        Reader.read(newDStinh)
              .forEach(s -> {
                  String[] l = s.split("\\|");
                  map.put(Long.valueOf(l[0]), new Province(l[0], l[1], l[2]));
              });
        return map;
    }

    // tao 1 file ds tinh moi: 30|HDG|Hải Dương
    private static void createNewDSTinh(String province, String fileDanhSachTinh, String newDStinh) throws IOException {
        Map<String, Province> map_province = new HashMap<>();
        Reader.read(fileDanhSachTinh)
              .forEach(s -> map(s, map_province));

        for (String line : Reader.read(province)) {
            List<String> items = getItems(line);
            String key = items.get(2);
            Province province1 = map_province.get(key);
            if (province1 != null) {
                province1.setId(items.get(1));
            }
            map_province.put(key, province1);
        }


        List<String> prLine = new ArrayList<>();
        map_province.forEach((s, p) -> {
            String lineProvinceOut = p.getId() + "|" + p.getCode()
                                                        .trim() + "|" + p.getName()
                                                                         .trim();
            prLine.add(lineProvinceOut);
        });

        Writer.write(prLine, newDStinh);
    }

    private static List<String> getItems(String line) {
        final String s = line.replaceAll("[^\\s]''", "");
        String regex_province = "'([^');]+)'|''";
        Matcher matcher = Pattern.compile(regex_province)
                                 .matcher(s);
        List<String> items = new ArrayList<>();
        while (matcher.find()) {
            items.add(matcher.group(1));
        }
        return items;
    }

    //vd: TTH | Thừa Thiên - Huế
    private static void map(String line, Map<String, Province> map) {
        String[] split = line.split("\\|");
        final String name = split[1].trim();
        final Province province = Province.builder()
                                          .code(split[0])
                                          .name(split[1])
                                          .build();
        map.put(name, province);

    }
}
