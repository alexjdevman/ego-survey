package ru.airlabs.ego.survey.service.impl;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.airlabs.ego.core.entity.Organization;
import ru.airlabs.ego.core.entity.OrganizationLocation;
import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.core.repository.OrganizationLocationRepository;
import ru.airlabs.ego.core.repository.OrganizationRepository;
import ru.airlabs.ego.core.repository.UserRepository;
import ru.airlabs.ego.survey.config.FileUploadConfig;
import ru.airlabs.ego.survey.dto.FileContent;
import ru.airlabs.ego.survey.dto.settings.AccountSettings;
import ru.airlabs.ego.survey.service.AccountSettingsService;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ru.airlabs.ego.survey.utils.ControllerUtils.GET_COMPANY_HR_IMAGE_REST_ADDRESS;
import static ru.airlabs.ego.survey.utils.ImageUtils.readImage;
import static ru.airlabs.ego.survey.utils.ImageUtils.scaleImage;

@Service("accountSettingsService")
@Transactional(readOnly = true)
public class AccountSettingsServiceImpl implements AccountSettingsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountSettingsServiceImpl.class);

    private static final String PREVIEW_IMAGE_PREFIX = "_preview";

    /**
     * Максимальный размер для фото HR компании (px)
     */
    private static final int MAX_IMAGE_SIZE = 300;

    /**
     * Максимальный размер для превью фото HR компании (px)
     */
    private static final int MAX_IMAGE_PREVIEW_SIZE = 72;

    /**
     * Расширение файла логотипа компании
     */
    private static final String PNG_EXTENSION = "png";

    /**
     * Конфигурация параметров загрузки файлов
     */
    private FileUploadConfig fileUploadConfig;

    /**
     * Репозиторий пользователей
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Репозиторий компаний
     */
    @Autowired
    private OrganizationRepository organizationRepository;

    /**
     * Репозиторий адресов компаний
     */
    @Autowired
    private OrganizationLocationRepository locationRepository;

    @Override
    public AccountSettings getAccountSettings(User currentUser) {
        final User user = userRepository.findById(currentUser.getId()).get();
        AccountSettings settings = new AccountSettings(user);
        Organization organization = organizationRepository.findByManagerId(currentUser.getId());
        if (organization != null) {
            fillOrganizationDetails(settings, organization);
        }
        return settings;
    }

    @Transactional
    @Override
    public void saveAccountSettings(AccountSettings settings, User currentUser) {
        final Long currentUserId = currentUser.getId();
        final User user = userRepository.findById(currentUserId).get();
        if (isNotBlank(settings.getManagerName())) {
            user.setName(settings.getManagerName());
        }
        if (isNotBlank(settings.getManagerEmail())) {
            user.setEmail(settings.getManagerEmail());
        }
        Organization organization = organizationRepository.findByManagerId(currentUserId);
        if (organization != null) {
            updateOrganization(organization, settings);
        } else {
            createOrganization(settings, currentUserId);
        }
    }

    @Override
    public void saveCompanyImage(Long companyId, MultipartFile file) {
        checkArgument(organizationRepository.existsById(companyId), "Компании с id %s не существует", companyId);
        final String imageRootLocation = fileUploadConfig.getCompanyLogoRootLocation();
        checkArgument(isNotBlank(imageRootLocation), "Не указан путь к корневой директории фото HR компаний");
        try (InputStream stream = new ByteArrayInputStream(file.getBytes())) {
            File rootDirectory = new File(imageRootLocation);
            if (!rootDirectory.exists()) rootDirectory.mkdirs();
            BufferedImage image = readImage(stream);
            // масштабируем изображение
            BufferedImage scaledImage = scaleImage(image, MAX_IMAGE_SIZE);
            File destFile = new File(buildDestFileName(rootDirectory, companyId, PNG_EXTENSION, false));
            // масштабируем изображение для превью
            BufferedImage previewImage = scaleImage(image, MAX_IMAGE_PREVIEW_SIZE);
            File previewFile = new File(buildDestFileName(rootDirectory, companyId, PNG_EXTENSION, true));
            // сохраняем изображения
            ImageIO.write(scaledImage, PNG_EXTENSION, destFile);
            ImageIO.write(previewImage, PNG_EXTENSION, previewFile);
        } catch (Exception err) {
            LOGGER.error(err.getMessage(), err);
            throw new RuntimeException(err);
        }
    }

    @Override
    public FileContent getCompanyImage(Long companyId, boolean preview) {
        checkArgument(organizationRepository.existsById(companyId), "Компании с id %s не существует", companyId);
        FileContent fileContent = new FileContent();
        final String imageRootLocation = fileUploadConfig.getCompanyLogoRootLocation();
        File imageDirectory = new File(imageRootLocation);
        if (!imageDirectory.exists()) {
            imageDirectory.mkdirs();
            return fileContent;
        }

        final String fileName = companyId + (preview ? PREVIEW_IMAGE_PREFIX : "") +
                "." + PNG_EXTENSION;
        final String filePath = imageDirectory.getAbsolutePath()
                + File.separator + fileName;
        File imageFile = new File(filePath);
        if (!imageFile.exists()) return fileContent;
        try (InputStream stream = new FileInputStream(imageFile)) {
            fileContent.fileName = fileName;
            fileContent.content = IOUtils.toByteArray(stream);
            return fileContent;
        } catch (Exception err) {
            LOGGER.error(err.getMessage(), err);
            throw new RuntimeException(err);
        }
    }

    /**
     * Проверка, загружено ли фото HR для компании
     *
     * @param companyId идентификатор компании
     * @return true - если загружен, false - в противном случае
     */
    public boolean isCompanyImageExists(Long companyId) {
        final String logoRootLocation = fileUploadConfig.getCompanyLogoRootLocation();
        File imageDirectory = new File(logoRootLocation);
        if (!imageDirectory.exists()) return Boolean.FALSE;

        final String fileName = companyId + "." + PNG_EXTENSION;
        final String filePath = imageDirectory.getAbsolutePath()
                + File.separator + fileName;

        return new File(filePath).exists();
    }

    private void updateOrganization(Organization organization, AccountSettings settings) {
        organization.setName(settings.getCompanyName());
        organization.setDescription(settings.getCompanyDescription());
        organization.setPhone(settings.getPhone());
        organization.setSite(settings.getSite());
        organization.setDateUpdate(new Date());
        OrganizationLocation location = locationRepository.findByManagerIdAndCompanyId(organization.getManagerId(),
                organization.getId());
        final String address = settings.getAddress();
        if (location != null) {
            location.setTitle(address);
        } else if (isNotBlank(address)) {
            createLocation(organization.getId(), organization.getManagerId(), address);
        }
    }

    private void createOrganization(AccountSettings settings, Long managerId) {
        Organization org = new Organization();
        org.setManagerId(managerId);
        org.setName(settings.getCompanyName());
        org.setDescription(settings.getCompanyDescription());
        org.setPhone(settings.getPhone());
        org.setSite(settings.getSite());
        org.setDateCreate(new Date());
        Organization savedOrg = organizationRepository.save(org);
        if (isNotBlank(settings.getAddress())) {
            createLocation(savedOrg.getId(), managerId, settings.getAddress());
        }
    }

    private void createLocation(Long organizationId, Long managerId, String address) {
        OrganizationLocation location = new OrganizationLocation();
        location.setManagerId(managerId);
        location.setCompanyId(organizationId);
        location.setTitle(address);
        location.setDateCreate(new Date());
        locationRepository.save(location);
    }

    private void fillOrganizationDetails(AccountSettings settings,
                                         Organization organization) {
        final Long organizationId = organization.getId();
        settings.setOrganizationId(organizationId);
        settings.setCompanyName(organization.getName());
        settings.setCompanyDescription(organization.getDescription());
        settings.setPhone(organization.getPhone());
        settings.setSite(organization.getSite());
        settings.setCompanyLogo(isCompanyImageExists(organizationId) ?
                format(GET_COMPANY_HR_IMAGE_REST_ADDRESS, organizationId, Boolean.FALSE.toString()) :
                null);
        OrganizationLocation orgLocation = locationRepository.findByManagerIdAndCompanyId(organization.getManagerId(),
                organizationId);
        if (orgLocation != null) {
            settings.setAddress(orgLocation.getTitle());
        }
    }

    private String buildDestFileName(File directory,
                                     Long companyId,
                                     String fileExtension,
                                     boolean preview) {
        return directory.getAbsolutePath() +
                File.separator +
                companyId + (preview ? PREVIEW_IMAGE_PREFIX : "") +
                "." + fileExtension;
    }

    @Resource(name = "fileUploadConfig")
    public void setFileUploadConfig(FileUploadConfig fileUploadConfig) {
        this.fileUploadConfig = fileUploadConfig;
    }

}
