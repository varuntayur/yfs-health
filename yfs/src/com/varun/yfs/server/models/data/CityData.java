package com.varun.yfs.server.models.data;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.dozer.Mapper;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.data.ModelData;
import com.varun.yfs.client.common.RpcStatusEnum;
import com.varun.yfs.client.index.ModelDataEnum;
import com.varun.yfs.dto.UserDTO;
import com.varun.yfs.server.models.City;
import com.varun.yfs.server.models.State;

public class CityData extends AbstractData
{
	private static final Logger LOGGER = Logger.getLogger(CityData.class);

	@Override
	public ModelData getModel(UserDTO userDto)
	{
		LOGGER.debug("Attempting data load");
		
		ModelData modelData = new BaseModelData();

		List<ModelData> list = DataUtil.<ModelData> getModelList(ModelDataEnum.City.name());
		modelData.set("data", list);
		modelData.set("parentStoreState", DataUtil.<ModelData> getModelList(ModelDataEnum.State.name()));

		modelData.set("configIds", Arrays.asList("cityName", "stateName"));
		modelData.set("configCols", Arrays.asList("City", "State"));
		modelData.set("configType", Arrays.asList("Text", "combo"));

		modelData.set("permissions", userDto.getEntityPermissionsMap().get(ModelDataEnum.City.name().toLowerCase()));
		
		LOGGER.debug("Data load complete.");
		return modelData;
	}

	@Override
	public RpcStatusEnum saveModel(ModelData model)
	{
		LOGGER.debug("Attempting to save model");
		
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction transact = session.beginTransaction();
		RpcStatusEnum status = RpcStatusEnum.FAILURE;
		try
		{
			List<State> lstStates = DataUtil.<State> getRawList("State");
			List<ModelData> lstModels = model.get("data");

			Mapper dozerMapper = HibernateUtil.getDozerMapper();

			for (ModelData modelData : lstModels)
			{
				City hibObject = dozerMapper.map(modelData, City.class);
				String stateName = modelData.get("stateName").toString();

				hibObject.setState(findParent(lstStates, new State(stateName)));

				if (hibObject.getId() <= 0)
				{
					hibObject.setName(modelData.get("cityName").toString());
					session.save(hibObject);

				} else
				{
					session.saveOrUpdate(hibObject);
				}
			}

			transact.commit();

			session.flush();
			session.close();

			status = RpcStatusEnum.SUCCESS;
			
			LOGGER.debug("Save model completed successfully.");
		} catch (HibernateException ex)
		{
			if (session != null)
			{
				transact.rollback();
				session.close();
			}
			LOGGER.error("Encountered error saving the model." + ex.getMessage());
			status = RpcStatusEnum.FAILURE;
		}
		return status;
	}

}
