package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import domain.target.TargetTableModel;

public class TargetDao {
	/**
	 * 
	 * @param model
	 * @return
	 */
	public synchronized boolean saveTargets(TargetTableModel model){
		try {
			Connection conn = getConnection();
			
			PreparedStatement pres = conn.prepareStatement("select ID From Targets");//会比select * From Targets 要快吧
			ResultSet rs = pres.executeQuery();
			String sql = "";
			if (rs.next()){
				sql = "update Targets SET Content=? where ID=1";
			}else{
				sql = "insert into Targets(ID,Content) values(1,?)";
			}
			
			pres=conn.prepareStatement(sql);//预编译

			String targetsJson = model.ToJson();
			pres.setString(1,targetsJson);
			int n = pres.executeUpdate();
			if (n==1){
				System.out.println("save targets successfully");
				return true;//不影响finally的执行
			}else {
				System.out.println("save targets failed");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
		return false;
	}
	
	/**
	 * 从数据库中读出存入的对象
	 */
	public TargetTableModel getTargets(){
		try {
			String sql="select * from Targets";
			conn = getConnection();
			pres=conn.prepareStatement(sql);
			ResultSet res=pres.executeQuery();
			while(res.next()){
				String Content =res.getString("Content");//获取content部分的内容
				return TargetTableModel.FromJson(Content);
			}
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
		return null;
	}
}
