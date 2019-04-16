package ru.airlabs.ego.survey.config;

/**
 * Бин для хранения настроек параметров загрузки файлов
 *
 * @author Aleksey Gorbachev
 */
public class FileUploadConfig {

    /**
     * Полный путь к корневой директории,
     * в которой будут храниться загружаемые фотографии пользователей
     */
    private String userImageRootLocation;

    /**
     * Полный путь к корневой директории,
     * в которой будут храниться логотипы компаний
     *
     */
    private String companyLogoRootLocation;

    /**
     * Полный путь к корневой директории,
     * в которой будут храниться загружаемые резюме пользователей
     */
    private String userResumeRootLocation;

    /**
     * Полный путь к корневой директории,
     * в которой будут храниться загружаемые резюме пользователей, которые не удалось обработать
     */
    private String userResumeErrorLocation;

    /**
     * Максимальное кол-во файлов, которые могут храниться в одной директории на сервере
     * (при превышении этого лимита будет создваться новая папка, например:
     * 1-20000 - папка {userImageRootLocation}/1/, 20001-40000 - папка {userImageRootLocation}/2/, и т.д.)
     */
    private Long maxDirectorySize;

    public String getUserImageRootLocation() {
        return userImageRootLocation;
    }

    public void setUserImageRootLocation(String userImageRootLocation) {
        this.userImageRootLocation = userImageRootLocation;
    }

    public String getCompanyLogoRootLocation() {
        return companyLogoRootLocation;
    }

    public void setCompanyLogoRootLocation(String companyLogoRootLocation) {
        this.companyLogoRootLocation = companyLogoRootLocation;
    }

    public String getUserResumeRootLocation() {
        return userResumeRootLocation;
    }

    public void setUserResumeRootLocation(String userResumeRootLocation) {
        this.userResumeRootLocation = userResumeRootLocation;
    }

    public String getUserResumeErrorLocation() {
        return userResumeErrorLocation;
    }

    public void setUserResumeErrorLocation(String userResumeErrorLocation) {
        this.userResumeErrorLocation = userResumeErrorLocation;
    }

    public Long getMaxDirectorySize() {
        return maxDirectorySize;
    }

    public void setMaxDirectorySize(Long maxDirectorySize) {
        this.maxDirectorySize = maxDirectorySize;
    }
}
