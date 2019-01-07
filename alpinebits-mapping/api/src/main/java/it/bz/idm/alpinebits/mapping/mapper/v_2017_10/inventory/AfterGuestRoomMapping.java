/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package it.bz.idm.alpinebits.mapping.mapper.v_2017_10.inventory;

import it.bz.idm.alpinebits.mapping.entity.inventory.GuestRoom;
import it.bz.idm.alpinebits.mapping.entity.inventory.ImageItem;
import it.bz.idm.alpinebits.mapping.entity.inventory.TextItemDescription;
import it.bz.idm.alpinebits.mapping.utils.CollectionUtils;
import it.bz.idm.alpinebits.xml.schema.v_2017_10.OTAHotelDescriptiveContentNotifRQ;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The herein declared methods are invoked after {@link GuestRoomMapper}
 * has finished to further customize the mapping.
 */
@Mapper
public abstract class AfterGuestRoomMapping {

    public static final int MULTIMEDIA_LONG_NAME_CODE = 25;
    public static final int MULTIMEDIA_DESCRIPTION_CODE = 1;
    public static final int MULTIMEDIA_PICTURES_CODE = 23;

    private final TextItemDescriptionMapper textItemDescriptionMapper = Mappers.getMapper(TextItemDescriptionMapper.class);
    private final ImageItemMapper imageItemMapper = Mappers.getMapper(ImageItemMapper.class);

    @AfterMapping
    public void updateGuestRoomMultimedia(
            @MappingTarget GuestRoom guestRoom,
            OTAHotelDescriptiveContentNotifRQ.HotelDescriptiveContents.HotelDescriptiveContent
                    .FacilityInfo.GuestRooms.GuestRoom otaGuestRoom
    ) {
        if (this.hasMultimediaDescriptions(otaGuestRoom)) {
            for (OTAHotelDescriptiveContentNotifRQ
                    .HotelDescriptiveContents
                    .HotelDescriptiveContent
                    .FacilityInfo
                    .GuestRooms
                    .GuestRoom
                    .MultimediaDescriptions
                    .MultimediaDescription multimediaDescription : otaGuestRoom.getMultimediaDescriptions().getMultimediaDescriptions()) {

                // Special case: Inventory/HotelInfo (push) client request hotel images
                if (multimediaDescription.getInfoCode() == null) {
                    List<ImageItem> hotelInfoImageItems = this.buildImageItems(multimediaDescription);
                    guestRoom.setHotelInfoPictures(hotelInfoImageItems);
                    continue;
                }

                // Long name
                if (multimediaDescription.getInfoCode().intValue() == MULTIMEDIA_LONG_NAME_CODE) {
                    List<TextItemDescription> textItemDescriptions = this.buildTextItemDescriptions(multimediaDescription);
                    guestRoom.setLongNames(textItemDescriptions);
                }

                // Description
                if (multimediaDescription.getInfoCode().intValue() == MULTIMEDIA_DESCRIPTION_CODE) {
                    List<TextItemDescription> textItemDescriptions = this.buildTextItemDescriptions(multimediaDescription);
                    guestRoom.setDescriptions(textItemDescriptions);
                }

                // Pictures
                if (multimediaDescription.getInfoCode().intValue() == MULTIMEDIA_PICTURES_CODE) {
                    List<ImageItem> imageItems = this.buildImageItems(multimediaDescription);
                    guestRoom.setPictures(imageItems);
                }
            }
        }
    }

    @AfterMapping
    public void updateOTAGuestRoomMultimedia(
            @MappingTarget OTAHotelDescriptiveContentNotifRQ
                    .HotelDescriptiveContents
                    .HotelDescriptiveContent
                    .FacilityInfo
                    .GuestRooms
                    .GuestRoom otaGuestRoom,
            GuestRoom guestRoom
    ) {
        List<OTAHotelDescriptiveContentNotifRQ
                .HotelDescriptiveContents
                .HotelDescriptiveContent
                .FacilityInfo
                .GuestRooms
                .GuestRoom
                .MultimediaDescriptions
                .MultimediaDescription> multimediaDescriptions = new ArrayList<>();

        // Special case: Inventory/HotelInfo (push) client request hotel images
        this.buildOTAMultiMediaImages(guestRoom.getHotelInfoPictures(), null)
                .ifPresent(multimediaDescriptions::add);

        this.buildOTAMultiMediaDescriptions(guestRoom.getLongNames(), MULTIMEDIA_LONG_NAME_CODE)
                .ifPresent(multimediaDescriptions::add);

        this.buildOTAMultiMediaDescriptions(guestRoom.getDescriptions(), MULTIMEDIA_DESCRIPTION_CODE)
                .ifPresent(multimediaDescriptions::add);

        this.buildOTAMultiMediaImages(guestRoom.getPictures(), MULTIMEDIA_PICTURES_CODE)
                .ifPresent(multimediaDescriptions::add);

        if (!multimediaDescriptions.isEmpty()) {
            OTAHotelDescriptiveContentNotifRQ
                    .HotelDescriptiveContents
                    .HotelDescriptiveContent
                    .FacilityInfo
                    .GuestRooms
                    .GuestRoom
                    .MultimediaDescriptions ms = new OTAHotelDescriptiveContentNotifRQ
                    .HotelDescriptiveContents
                    .HotelDescriptiveContent
                    .FacilityInfo
                    .GuestRooms.GuestRoom
                    .MultimediaDescriptions();
            ms.getMultimediaDescriptions().addAll(multimediaDescriptions);
            otaGuestRoom.setMultimediaDescriptions(ms);
        }
    }

    @AfterMapping
    public void updateOTAGuestRoomAmenitites(
            @MappingTarget OTAHotelDescriptiveContentNotifRQ
                    .HotelDescriptiveContents
                    .HotelDescriptiveContent
                    .FacilityInfo
                    .GuestRooms
                    .GuestRoom otaGuestRoom,
            GuestRoom guestRoom
    ) {
        if (CollectionUtils.isNullOrEmpty(guestRoom.getRoomAmenityCodes())) {
            otaGuestRoom.setAmenities(null);
        }
    }

    private boolean hasMultimediaDescriptions(OTAHotelDescriptiveContentNotifRQ
                                                      .HotelDescriptiveContents
                                                      .HotelDescriptiveContent
                                                      .FacilityInfo
                                                      .GuestRooms
                                                      .GuestRoom otaGuestRoom) {
        return otaGuestRoom.getMultimediaDescriptions() != null
                && !CollectionUtils.isNullOrEmpty(otaGuestRoom.getMultimediaDescriptions().getMultimediaDescriptions());
    }

    private List<TextItemDescription> buildTextItemDescriptions(OTAHotelDescriptiveContentNotifRQ.HotelDescriptiveContents
                                                                        .HotelDescriptiveContent.FacilityInfo.GuestRooms
                                                                        .GuestRoom.MultimediaDescriptions.MultimediaDescription multimediaDescription) {
        if (!hasTextItems(multimediaDescription)) {
            return Collections.emptyList();
        }

        return multimediaDescription.getTextItems().getTextItem().getDescriptions().stream()
                .map(this.textItemDescriptionMapper::toTextItemDescription)
                .collect(Collectors.toList());
    }

    private boolean hasTextItems(OTAHotelDescriptiveContentNotifRQ.HotelDescriptiveContents.HotelDescriptiveContent.FacilityInfo
                                         .GuestRooms.GuestRoom.MultimediaDescriptions.MultimediaDescription multimediaDescription) {
        return multimediaDescription.getTextItems() != null
                && multimediaDescription.getTextItems().getTextItem() != null
                && !CollectionUtils.isNullOrEmpty(multimediaDescription.getTextItems().getTextItem().getDescriptions());
    }

    private List<ImageItem> buildImageItems(OTAHotelDescriptiveContentNotifRQ.HotelDescriptiveContents
                                                    .HotelDescriptiveContent.FacilityInfo.GuestRooms
                                                    .GuestRoom.MultimediaDescriptions.MultimediaDescription multimediaDescription) {
        if (!hasImageItems(multimediaDescription)) {
            return Collections.emptyList();
        }

        return multimediaDescription.getImageItems().getImageItems().stream()
                .map(this.imageItemMapper::toImageItem)
                .collect(Collectors.toList());
    }

    private boolean hasImageItems(OTAHotelDescriptiveContentNotifRQ.HotelDescriptiveContents.HotelDescriptiveContent.FacilityInfo
                                          .GuestRooms.GuestRoom.MultimediaDescriptions.MultimediaDescription multimediaDescription) {
        return multimediaDescription.getImageItems() != null
                && !CollectionUtils.isNullOrEmpty(multimediaDescription.getImageItems().getImageItems());
    }

    private Optional<OTAHotelDescriptiveContentNotifRQ
            .HotelDescriptiveContents
            .HotelDescriptiveContent
            .FacilityInfo
            .GuestRooms
            .GuestRoom
            .MultimediaDescriptions
            .MultimediaDescription> buildOTAMultiMediaDescriptions(
            List<TextItemDescription> textItemDescriptions,
            int code
    ) {
        if (CollectionUtils.isNullOrEmpty(textItemDescriptions)) {
            return Optional.empty();
        }

        List<OTAHotelDescriptiveContentNotifRQ
                .HotelDescriptiveContents
                .HotelDescriptiveContent
                .FacilityInfo
                .GuestRooms
                .GuestRoom
                .MultimediaDescriptions
                .MultimediaDescription
                .TextItems
                .TextItem
                .Description> descriptions = textItemDescriptions.stream()
                .map(this.textItemDescriptionMapper::toOTATextItemDescription)
                .collect(Collectors.toList());

        OTAHotelDescriptiveContentNotifRQ
                .HotelDescriptiveContents
                .HotelDescriptiveContent
                .FacilityInfo
                .GuestRooms
                .GuestRoom
                .MultimediaDescriptions
                .MultimediaDescription
                .TextItems
                .TextItem textItem = this.buildEmptyOTATextItem();
        textItem.getDescriptions().addAll(descriptions);

        OTAHotelDescriptiveContentNotifRQ
                .HotelDescriptiveContents
                .HotelDescriptiveContent
                .FacilityInfo
                .GuestRooms
                .GuestRoom
                .MultimediaDescriptions
                .MultimediaDescription
                .TextItems textItems = this.buildEmptyOTATextItems();
        textItems.setTextItem(textItem);

        OTAHotelDescriptiveContentNotifRQ
                .HotelDescriptiveContents
                .HotelDescriptiveContent
                .FacilityInfo
                .GuestRooms
                .GuestRoom
                .MultimediaDescriptions
                .MultimediaDescription multimediaDescription = this.buildEmptyOTAMultimediaDescription();
        multimediaDescription.setTextItems(textItems);
        multimediaDescription.setInfoCode(BigInteger.valueOf(code));
        return Optional.of(multimediaDescription);
    }

    private Optional<OTAHotelDescriptiveContentNotifRQ
            .HotelDescriptiveContents
            .HotelDescriptiveContent
            .FacilityInfo
            .GuestRooms
            .GuestRoom
            .MultimediaDescriptions
            .MultimediaDescription> buildOTAMultiMediaImages(
            List<ImageItem> imageItems,
            Integer code
    ) {
        if (CollectionUtils.isNullOrEmpty(imageItems)) {
            return Optional.empty();
        }

        List<OTAHotelDescriptiveContentNotifRQ
                .HotelDescriptiveContents
                .HotelDescriptiveContent
                .FacilityInfo
                .GuestRooms
                .GuestRoom
                .MultimediaDescriptions
                .MultimediaDescription
                .ImageItems
                .ImageItem> images = imageItems.stream()
                .map(this.imageItemMapper::toOTAImageItemm)
                .collect(Collectors.toList());

        OTAHotelDescriptiveContentNotifRQ
                .HotelDescriptiveContents
                .HotelDescriptiveContent
                .FacilityInfo
                .GuestRooms
                .GuestRoom
                .MultimediaDescriptions
                .MultimediaDescription
                .ImageItems otaImageItems = this.buildEmptyOTAImageItems();
        otaImageItems.getImageItems().addAll(images);

        OTAHotelDescriptiveContentNotifRQ
                .HotelDescriptiveContents
                .HotelDescriptiveContent
                .FacilityInfo
                .GuestRooms
                .GuestRoom
                .MultimediaDescriptions
                .MultimediaDescription multimediaDescription = this.buildEmptyOTAMultimediaDescription();
        multimediaDescription.setImageItems(otaImageItems);

        if (code != null) {
            multimediaDescription.setInfoCode(BigInteger.valueOf(code));
        }
        return Optional.of(multimediaDescription);
    }

    private OTAHotelDescriptiveContentNotifRQ
            .HotelDescriptiveContents
            .HotelDescriptiveContent
            .FacilityInfo
            .GuestRooms
            .GuestRoom
            .MultimediaDescriptions
            .MultimediaDescription
            .TextItems buildEmptyOTATextItems() {
        return new OTAHotelDescriptiveContentNotifRQ
                .HotelDescriptiveContents
                .HotelDescriptiveContent
                .FacilityInfo
                .GuestRooms
                .GuestRoom
                .MultimediaDescriptions
                .MultimediaDescription
                .TextItems();
    }

    private OTAHotelDescriptiveContentNotifRQ
            .HotelDescriptiveContents
            .HotelDescriptiveContent
            .FacilityInfo
            .GuestRooms
            .GuestRoom
            .MultimediaDescriptions
            .MultimediaDescription
            .TextItems
            .TextItem buildEmptyOTATextItem() {
        return new OTAHotelDescriptiveContentNotifRQ
                .HotelDescriptiveContents
                .HotelDescriptiveContent
                .FacilityInfo
                .GuestRooms
                .GuestRoom
                .MultimediaDescriptions
                .MultimediaDescription
                .TextItems
                .TextItem();
    }


    private OTAHotelDescriptiveContentNotifRQ
            .HotelDescriptiveContents
            .HotelDescriptiveContent
            .FacilityInfo
            .GuestRooms
            .GuestRoom
            .MultimediaDescriptions
            .MultimediaDescription
            .ImageItems buildEmptyOTAImageItems() {
        return new OTAHotelDescriptiveContentNotifRQ
                .HotelDescriptiveContents
                .HotelDescriptiveContent
                .FacilityInfo
                .GuestRooms
                .GuestRoom
                .MultimediaDescriptions
                .MultimediaDescription
                .ImageItems();
    }

    private OTAHotelDescriptiveContentNotifRQ
            .HotelDescriptiveContents
            .HotelDescriptiveContent
            .FacilityInfo
            .GuestRooms
            .GuestRoom
            .MultimediaDescriptions
            .MultimediaDescription buildEmptyOTAMultimediaDescription() {
        return new OTAHotelDescriptiveContentNotifRQ
                .HotelDescriptiveContents
                .HotelDescriptiveContent
                .FacilityInfo
                .GuestRooms
                .GuestRoom
                .MultimediaDescriptions
                .MultimediaDescription();
    }
}
